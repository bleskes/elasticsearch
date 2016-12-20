#ifndef RAPIDJSON_PULLREADER_H_
#define RAPIDJSON_PULLREADER_H_

// Written in the style of RapidJson
// Version 0.1

#include "rapidjson.h"
#include "reader.h"
#include "internal/pow10.h"
#include "internal/stack.h"

#ifndef RAPIDJSON_PULL_ERROR
#define RAPIDJSON_PULL_ERROR(msg, offset) do { parseError_ = msg; errorOffset_ = offset; } while(false)
#endif

namespace rapidjson {

///////////////////////////////////////////////////////////////////////////////
// TokenType

//! Type of JSON token
enum TokenType {
	kTokenNull = 0,
	kTokenBool = 1,
	kTokenInt = 2,
	kTokenUInt = 3,
	kTokenInt64 = 4,
	kTokenUInt64 = 5,
	kTokenDouble = 6,
	kTokenString = 7,
	kTokenObjectStart = 8,
	kTokenObjectEnd = 9,
	kTokenArrayStart = 10,
	kTokenArrayEnd = 11,
	// The following are not returned, but are used in validation
	kTokenRoot = 12,
	kTokenColon = 13,
	kTokenComma = 14
};

///////////////////////////////////////////////////////////////////////////////
// GenericToken type

//! Token information
template <typename Encoding>
struct GenericToken {
	typedef typename Encoding::Ch Ch;

	TokenType type;
	SizeType length;	//!< Only used for strings
	union {
		bool b;
		int i;
		unsigned u;
		int64_t i64;
		uint64_t u64;
		double d;
		const Ch *str;
	};
};

//! Token with UTF8 encoding for strings.
typedef GenericToken<UTF8<> > Token;

///////////////////////////////////////////////////////////////////////////////
// GenericPullReader

//! Pull style JSON parser. Use PullReader for UTF8 encoding and default allocator.
/*! GenericPullReader parses JSON text from a stream, and returns single tokens
	synchronously to the caller.

	The usage pattern is similar to that of the Jackson Java library.

    It needs to allocate a stack for storing a single decoded string during
    non-destructive parsing.

    For in-situ parsing, the decoded string is directly written to the source
    text string, no temporary buffer is required.

    A GenericPullReader object can be reused for parsing multiple JSON text.

    \tparam Encoding Encoding of both the stream and the parse output.
    \tparam Allocator Allocator type for stack.
*/
template <typename Encoding, typename Allocator = MemoryPoolAllocator<> >
class GenericPullReader {
public:
	typedef typename Encoding::Ch Ch;
	typedef GenericToken<Encoding> Token;

	//! Constructor.
	/*! \param allocator Optional allocator for allocating stack memory. (Only use for non-destructive parsing)
		\param stackCapacity stack capacity in bytes for storing a single decoded string.  (Only use for non-destructive parsing)
	*/
	GenericPullReader(Allocator* allocator = 0, size_t stackCapacity = kDefaultStackCapacity) :
		stack_(allocator, stackCapacity), nested_(allocator, stackCapacity), parseError_(0),
		 errorOffset_(0), lastType_(kTokenRoot) {}

	//! Parse the first JSON value
	/*! \tparam parseFlags Combination of ParseFlag.
		 \tparam Stream Type of input stream.
		 \param stream Input stream to be parsed.
		 \param token The object to receive token information.
		 \return Whether the parsing is successful.
	*/
	template<unsigned parseFlags, typename Stream>
	bool ParseFirst(Stream& stream, Token& token) {
		parseError_ = 0;
		errorOffset_ = 0;
		stack_.Clear();
		nested_.Clear();

		SkipWhitespace(stream);

		if (stream.Peek() == '\0') {
			RAPIDJSON_PULL_ERROR("Text only contains white space(s)", stream.Tell());
			return false;
		}

		switch (stream.Peek()) {
			case '{': return ParseObjectStart<parseFlags>(stream, token);
			case '[': return ParseArrayStart <parseFlags>(stream, token);
		}

		RAPIDJSON_PULL_ERROR("Expect either an object or array at root", stream.Tell());
		return false;
	}

	//! Parse the next JSON value
	/*! \tparam parseFlags Combination of ParseFlag.
		 \tparam Stream Type of input stream.
		 \param stream Input stream to be parsed.
		 \param token The object to receive token information.
		 \return Whether the parsing is successful.
	*/
	template<unsigned parseFlags, typename Stream>
	bool ParseNext(Stream& stream, Token& token) {
		if (parseError_ != 0)
		{
			return false;
		}

		SkipWhitespace(stream);

		switch (stream.Peek()) {
			case '{': return ParseObjectStart<parseFlags>(stream, token);
			case '}': return ParseObjectEnd  <parseFlags>(stream, token);
			case '[': return ParseArrayStart <parseFlags>(stream, token);
			case ']': return ParseArrayEnd   <parseFlags>(stream, token);
			case ':': return ParseColon      <parseFlags>(stream, token);
			case ',': return ParseComma      <parseFlags>(stream, token);
			case '\0': return false;
		}
		return ParseValue<parseFlags>(stream, token);
	}

	bool HasParseError() const { return parseError_ != 0; }
	const char* GetParseError() const { return parseError_; }
	size_t GetErrorOffset() const { return errorOffset_; }

private:
	// Parse object start: {
	template<unsigned parseFlags, typename Stream>
	bool ParseObjectStart(Stream& stream, Token& token) {
		RAPIDJSON_ASSERT(stream.Peek() == '{');

		// Objects separated by commas are only allowed in arrays
		if (lastType_ == kTokenComma)
		{
			if (*nested_.template Top<Ch>() != '[')  // peek
			{
				RAPIDJSON_PULL_ERROR("Object start after a comma must be in an array", stream.Tell());
				return false;
			}

		}
		else if (lastType_ != kTokenColon && lastType_ != kTokenRoot && lastType_ != kTokenArrayStart)
		{
			RAPIDJSON_PULL_ERROR("Object start must be at root or follow colon or an array start", stream.Tell());
			return false;
		}

		*nested_.template Push<Ch>() = stream.Take();	// Skip '{'

		lastType_ = token.type = kTokenObjectStart;

		return true;
	}

	// Parse object end: }
	template<unsigned parseFlags, typename Stream>
	bool ParseObjectEnd(Stream& stream, Token& token) {
		RAPIDJSON_ASSERT(stream.Peek() == '}');

		if (lastType_ == kTokenArrayStart || lastType_ == kTokenColon || lastType_ == kTokenComma || lastType_ == kTokenRoot)
		{
			RAPIDJSON_PULL_ERROR("Object end may not follow array start, colon or comma", stream.Tell());
			return false;
		}

		if (*nested_.template Pop<Ch>(1) != '{')
		{
			RAPIDJSON_PULL_ERROR("Object end does not match object start", stream.Tell());
			return false;
		}

		stream.Take();	// Skip '}'

		lastType_ = token.type = kTokenObjectEnd;

		if (nested_.GetSize() == 0)
		{
			SkipWhitespace(stream);

			if (stream.Peek() != '\0')
				RAPIDJSON_PULL_ERROR("Nothing should follow the root object.", stream.Tell());
		}

		return true;
	}

	// Parse array start: [
	template<unsigned parseFlags, typename Stream>
	bool ParseArrayStart(Stream& stream, Token& token) {
		RAPIDJSON_ASSERT(stream.Peek() == '[');

		if (lastType_ != kTokenColon && lastType_ != kTokenRoot)
		{
			RAPIDJSON_PULL_ERROR("Array start must be at root or follow colon", stream.Tell());
			return false;
		}

		*nested_.template Push<Ch>() = stream.Take();	// Skip '['

		lastType_ = token.type = kTokenArrayStart;

		return true;
	}

	// Parse array end: ]
	template<unsigned parseFlags, typename Stream>
	bool ParseArrayEnd(Stream& stream, Token& token) {
		RAPIDJSON_ASSERT(stream.Peek() == ']');

		if (lastType_ == kTokenObjectStart || lastType_ == kTokenComma || lastType_ == kTokenRoot)
		{
			RAPIDJSON_PULL_ERROR("Array end may not follow object start or comma", stream.Tell());
			return false;
		}

		if (*nested_.template Pop<Ch>(1) != '[')
		{
			RAPIDJSON_PULL_ERROR("Array end does not match array start", stream.Tell());
			return false;
		}

		stream.Take();	// Skip ']'

		lastType_ = token.type = kTokenArrayEnd;

		if (nested_.GetSize() == 0)
		{
			SkipWhitespace(stream);

			if (stream.Peek() != '\0')
				RAPIDJSON_PULL_ERROR("Nothing should follow the root array.", stream.Tell());
		}

		return true;
	}

	template<unsigned parseFlags, typename Stream>
	bool ParseNull(Stream& stream, Token& token) {
		RAPIDJSON_ASSERT(stream.Peek() == 'n');
		stream.Take();

		if (stream.Take() == 'u' && stream.Take() == 'l' && stream.Take() == 'l')
		{
			lastType_ = token.type = kTokenNull;
			return true;
		}

		RAPIDJSON_PULL_ERROR("Invalid value", stream.Tell() - 1);
		return false;
	}

	template<unsigned parseFlags, typename Stream>
	bool ParseTrue(Stream& stream, Token& token) {
		RAPIDJSON_ASSERT(stream.Peek() == 't');
		stream.Take();

		if (stream.Take() == 'r' && stream.Take() == 'u' && stream.Take() == 'e')
		{
			lastType_ = token.type = kTokenBool;
			token.b = true;
			return true;
		}

		RAPIDJSON_PULL_ERROR("Invalid value", stream.Tell());
		return false;
	}

	template<unsigned parseFlags, typename Stream>
	bool ParseFalse(Stream& stream, Token& token) {
		RAPIDJSON_ASSERT(stream.Peek() == 'f');
		stream.Take();

		if (stream.Take() == 'a' && stream.Take() == 'l' && stream.Take() == 's' && stream.Take() == 'e')
		{
			lastType_ = token.type = kTokenBool;
			token.b = false;
			return true;
		}

		RAPIDJSON_PULL_ERROR("Invalid value", stream.Tell() - 1);
		return false;
	}

	// Helper function to parse four hexidecimal digits in \uXXXX in ParseString().
	template<typename Stream>
	unsigned ParseHex4(Stream& stream) {
		Stream s = stream;	// Use a local copy for optimization
		unsigned codepoint = 0;
		for (int i = 0; i < 4; i++) {
			Ch c = s.Take();
			codepoint <<= 4;
			codepoint += c;
			if (c >= '0' && c <= '9')
				codepoint -= '0';
			else if (c >= 'A' && c <= 'F')
				codepoint -= 'A' - 10;
			else if (c >= 'a' && c <= 'f')
				codepoint -= 'a' - 10;
			else {
				RAPIDJSON_PULL_ERROR("Incorrect hex digit after \\u escape", s.Tell() - 1);
				return 0;
			}
		}
		stream = s; // Restore stream
		return codepoint;
	}

	// Parse string, handling the prefix and suffix double quotes and escaping.
	template<unsigned parseFlags, typename Stream>
	bool ParseString(Stream& stream, Token& token) {
#define Z16 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
		static const Ch escape[256] = {
			Z16, Z16, 0, 0,'\"', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,'/',
			Z16, Z16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,'\\', 0, 0, 0,
			0, 0,'\b', 0, 0, 0,'\f', 0, 0, 0, 0, 0, 0, 0,'\n', 0,
			0, 0,'\r', 0,'\t', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			Z16, Z16, Z16, Z16, Z16, Z16, Z16, Z16
		};
#undef Z16

		Stream s = stream;	// Use a local copy for optimization
		RAPIDJSON_ASSERT(s.Peek() == '\"');
		s.Take();	// Skip '\"'
		Ch *head;
		SizeType len;
		if (parseFlags & kParseInsituFlag)
			head = s.PutBegin();
		else
			len = 0;

#define RAPIDJSON_PUT(x) \
	do { \
		if (parseFlags & kParseInsituFlag) \
			s.Put(x); \
		else { \
			*stack_.template Push<Ch>() = x; \
			++len; \
		} \
	} while(false)

		for (;;) {
			Ch c = s.Take();
			if (c == '\\') {	// Escape
				Ch e = s.Take();
				if ((sizeof(Ch) == 1 || static_cast<unsigned int>(e) < 256) && escape[static_cast<unsigned char>(e)])
					RAPIDJSON_PUT(escape[static_cast<unsigned char>(e)]);
				else if (e == 'u') {	// Unicode
					unsigned codepoint = ParseHex4(s);
					if (codepoint >= 0xD800 && codepoint <= 0xDBFF) { // Handle UTF-16 surrogate pair
						if (s.Take() != '\\' || s.Take() != 'u') {
							RAPIDJSON_PULL_ERROR("Missing the second \\u in surrogate pair", s.Tell() - 2);
							return false;
						}
						unsigned codepoint2 = ParseHex4(s);
						if (codepoint2 < 0xDC00 || codepoint2 > 0xDFFF) {
							RAPIDJSON_PULL_ERROR("The second \\u in surrogate pair is invalid", s.Tell() - 2);
							return false;
						}
						codepoint = (((codepoint - 0xD800) << 10) | (codepoint2 - 0xDC00)) + 0x10000;
					}

					Ch buffer[4];
					SizeType count = SizeType(Encoding::Encode(buffer, codepoint) - &buffer[0]);

					if (parseFlags & kParseInsituFlag)
						for (SizeType i = 0; i < count; i++)
							s.Put(buffer[i]);
					else {
						memcpy(stack_.template Push<Ch>(count), buffer, count * sizeof(Ch));
						len += count;
					}
				}
				else {
					RAPIDJSON_PULL_ERROR("Unknown escape character", stream.Tell() - 1);
					return false;
				}
			}
			else if (c == '"') {	// Closing double quote
				lastType_ = token.type = kTokenString;
				if (parseFlags & kParseInsituFlag) {
					size_t length = s.PutEnd(head);
					RAPIDJSON_ASSERT(length <= 0xFFFFFFFF);
					RAPIDJSON_PUT('\0');	// null-terminate the string
					token.str = head;
					token.length = SizeType(length);
				}
				else {
					RAPIDJSON_PUT('\0');
					token.str = stack_.template Pop<Ch>(len);
					token.length = len - 1;
				}
				stream = s;	// restore stream
				break;
			}
			else if (c == '\0') {
				RAPIDJSON_PULL_ERROR("lacks ending quotation before the end of string", stream.Tell() - 1);
				return false;
			}
			else if (static_cast<unsigned int>(c) < 0x20) {	// RFC 4627: unescaped = %x20-21 / %x23-5B / %x5D-10FFFF
				RAPIDJSON_PULL_ERROR("Incorrect unescaped character in string", stream.Tell() - 1);
				return false;
			}
			else
				RAPIDJSON_PUT(c);	// Normal character, just copy
		}
		return true;
#undef RAPIDJSON_PUT
	}

	template<unsigned parseFlags, typename Stream>
	bool ParseNumber(Stream& stream, Token& token) {
		Stream s = stream; // Local copy for optimization
		// Parse minus
		bool minus = false;
		if (s.Peek() == '-') {
			minus = true;
			s.Take();
		}

		// Parse int: zero / ( digit1-9 *DIGIT )
		unsigned i;
		bool try64bit = false;
		if (s.Peek() == '0') {
			i = 0;
			s.Take();
		}
		else if (s.Peek() >= '1' && s.Peek() <= '9') {
			i = s.Take() - '0';

			if (minus)
				while (s.Peek() >= '0' && s.Peek() <= '9') {
					if (i >= 214748364) { // 2^31 = 2147483648
						if (i != 214748364 || s.Peek() > '8') {
							try64bit = true;
							break;
						}
					}
					i = i * 10 + (s.Take() - '0');
				}
			else
				while (s.Peek() >= '0' && s.Peek() <= '9') {
					if (i >= 429496729) { // 2^32 - 1 = 4294967295
						if (i != 429496729 || s.Peek() > '5') {
							try64bit = true;
							break;
						}
					}
					i = i * 10 + (s.Take() - '0');
				}
		}
		else {
			RAPIDJSON_PULL_ERROR("Expect a value here.", stream.Tell());
			return false;
		}

		// Parse 64bit int
		uint64_t i64(0);
		bool useDouble = false;
		if (try64bit) {
			i64 = i;
			if (minus)
				while (s.Peek() >= '0' && s.Peek() <= '9') {
					if (i64 >= 922337203685477580uLL) // 2^63 = 9223372036854775808
						if (i64 != 922337203685477580uLL || s.Peek() > '8') {
							useDouble = true;
							break;
						}
					i64 = i64 * 10 + (s.Take() - '0');
				}
			else
				while (s.Peek() >= '0' && s.Peek() <= '9') {
					if (i64 >= 1844674407370955161uLL) // 2^64 - 1 = 18446744073709551615
						if (i64 != 1844674407370955161uLL || s.Peek() > '5') {
							useDouble = true;
							break;
						}
					i64 = i64 * 10 + (s.Take() - '0');
				}
		}

		// Force double for big integer
		double d(0.0);
		if (useDouble) {
			d = static_cast<double>(i64);
			while (s.Peek() >= '0' && s.Peek() <= '9') {
				if (d >= 1E307) {
					RAPIDJSON_PULL_ERROR("Number too big to store in double", stream.Tell());
					return false;
				}
				d = d * 10 + (s.Take() - '0');
			}
		}

		// Parse frac = decimal-point 1*DIGIT
		int expFrac = 0;
		if (s.Peek() == '.') {
			if (!useDouble) {
				d = try64bit ? static_cast<double>(i64) : static_cast<double>(i);
				useDouble = true;
			}
			s.Take();

			if (s.Peek() >= '0' && s.Peek() <= '9') {
				d = d * 10 + (s.Take() - '0');
				--expFrac;
			}
			else {
				RAPIDJSON_PULL_ERROR("At least one digit in fraction part", stream.Tell());
				return false;
			}

			while (s.Peek() >= '0' && s.Peek() <= '9') {
				if (expFrac > -16) {
					d = d * 10 + (s.Peek() - '0');
					--expFrac;
				}
				s.Take();
			}
		}

		// Parse exp = e [ minus / plus ] 1*DIGIT
		int exp = 0;
		if (s.Peek() == 'e' || s.Peek() == 'E') {
			if (!useDouble) {
				d = try64bit ? static_cast<double>(i64) : static_cast<double>(i);
				useDouble = true;
			}
			s.Take();

			bool expMinus = false;
			if (s.Peek() == '+')
				s.Take();
			else if (s.Peek() == '-') {
				s.Take();
				expMinus = true;
			}

			if (s.Peek() >= '0' && s.Peek() <= '9') {
				exp = s.Take() - '0';
				while (s.Peek() >= '0' && s.Peek() <= '9') {
					exp = exp * 10 + (s.Take() - '0');
					if (exp > 308) {
						RAPIDJSON_PULL_ERROR("Number too big to store in double", stream.Tell());
						return false;
					}
				}
			}
			else {
				RAPIDJSON_PULL_ERROR("At least one digit in exponent", s.Tell());
				return false;
			}

			if (expMinus)
				exp = -exp;
		}

		// Finish parsing, call event according to the type of number.
		if (useDouble) {
			d *= internal::Pow10(exp + expFrac);
			lastType_ = token.type = kTokenDouble;
			token.d = minus ? -d : d;
		}
		else {
			if (try64bit) {
				if (minus)
				{
					lastType_ = token.type = kTokenInt64;
					token.i64 = -static_cast<int64_t>(i64);
				}
				else
				{
					lastType_ = token.type = kTokenUInt64;
					token.u64 = i64;
				}
			}
			else {
				if (minus)
				{
					lastType_ = token.type = kTokenInt;
					token.i64 = -static_cast<int>(i);
				}
				else
				{
					lastType_ = token.type = kTokenUInt;
					token.u = i;
				}
			}
		}

		stream = s; // restore stream

		return true;
	}

	template<unsigned parseFlags, typename Stream>
	bool ParseColon(Stream& stream, Token& token) {
		RAPIDJSON_ASSERT(stream.Peek() == ':');

		if (lastType_ != kTokenString)
		{
			RAPIDJSON_PULL_ERROR("Colon must follow string", stream.Tell());
			return false;
		}

		stream.Take();

		lastType_ = kTokenColon;

		return ParseNext<parseFlags>(stream, token);
	}

	template<unsigned parseFlags, typename Stream>
	bool ParseComma(Stream& stream, Token& token) {
		RAPIDJSON_ASSERT(stream.Peek() == ',');

		if (lastType_ == kTokenArrayStart || lastType_ == kTokenArrayStart || lastType_ == kTokenColon || lastType_ == kTokenComma || lastType_ == kTokenRoot)
		{
			RAPIDJSON_PULL_ERROR("Comma must follow value", stream.Tell());
			return false;
		}

		stream.Take();

		lastType_ = kTokenComma;

		return ParseNext<parseFlags>(stream, token);
	}

	template<unsigned parseFlags, typename Stream>
	bool ParseValue(Stream& stream, Token& token) {
		if (lastType_ != kTokenObjectStart && lastType_ != kTokenArrayStart && lastType_ != kTokenColon && lastType_ != kTokenComma)
		{
			RAPIDJSON_PULL_ERROR("Value must follow colon, comma, array start or object start - ", stream.Tell());
			return false;
		}

		if (stream.Peek() != '"' && lastType_ != kTokenColon && *nested_.template Top<Ch>() == '{')
		{
			RAPIDJSON_PULL_ERROR("Only strings may be object names", stream.Tell());
			return false;
		}

		switch (stream.Peek()) {
			case 'n': return ParseNull       <parseFlags>(stream, token);
			case 't': return ParseTrue       <parseFlags>(stream, token);
			case 'f': return ParseFalse      <parseFlags>(stream, token);
			case '"': return ParseString     <parseFlags>(stream, token);
		}
		return ParseNumber<parseFlags>(stream, token);
	}

private:
	static const size_t kDefaultStackCapacity = 256;	//!< Default stack capacity in bytes for storing a single decoded string.
	internal::Stack<Allocator> stack_;	//!< A stack for storing decoded string temporarily during non-destructive parsing.
	internal::Stack<Allocator> nested_;	//!< A stack for storing nesting levels of objects and arrays for validation.
	const char* parseError_;
	size_t errorOffset_;
	TokenType lastType_;
}; // class GenericPullReader

//! Pull reader with UTF8 encoding and default allocator.
typedef GenericPullReader<UTF8<> > PullReader;

} // namespace rapidjson

#endif // RAPIDJSON_PULLREADER_H_
