#ifndef RAPIDJSON_GENERICREADSTREAM_H_
#define RAPIDJSON_GENERICREADSTREAM_H_

#include "rapidjson.h"
#include <istream>
#include <streambuf>
#include <string>

namespace rapidjson {

//! Wrapper of std::istream for input.
class GenericReadStream {
public:
    typedef char Ch;    //!< Character type (byte).

    //! Constructor.
    /*!
        \param is Input stream.
    */
    GenericReadStream(std::istream& is) : is_(&is), count_(0) {
		Read();
    }

    Ch Peek() const {
		return current_;
    }

    Ch Take() {
		char c = current_;
		Read();
		return c;
    }

    size_t Tell() const {
        return count_;
    }

#ifdef NDEBUG
// Assertions are disabled so the methods below are no-ops
#define RETURN_ATTRIBUTE
#else
// This avoids compiler warnings when building with assertions enabled
#ifdef Windows
#define RETURN_ATTRIBUTE __declspec(noreturn)
#else
#define RETURN_ATTRIBUTE __attribute__ ((noreturn))
#endif
#endif

    // Not implemented
    RETURN_ATTRIBUTE void Put(Ch /*c*/) { RAPIDJSON_ASSERT(false); }
    RETURN_ATTRIBUTE void Flush() { RAPIDJSON_ASSERT(false); }
    Ch* PutBegin() { RAPIDJSON_ASSERT(false); return 0; }
    size_t PutEnd(Ch*) { RAPIDJSON_ASSERT(false); return 0; }

#undef RETURN_ATTRIBUTE

private:
    typedef std::char_traits<char> Traits;

	void Read() {
		RAPIDJSON_ASSERT(is_->rdbuf() != 0);
		// Read from the underlying stream buffer to avoid sentry overhead
		int c = is_->rdbuf()->sbumpc();
		if (c != Traits::eof()) {
			current_ = static_cast<char>(c);
			++count_;
		}
		else {
			current_ = '\0';
			// We must set the EOF on the wrapping stream since we're
			// manipulating its buffer without its knowledge
			is_->setstate(std::ios_base::eofbit);
		}
	}

	// This is stored as a pointer because the Reader class requires that read
	// streams have an assignment operator
	std::istream* is_;
	char current_;
	size_t count_;
};

} // namespace rapidjson

#endif // RAPIDJSON_GENERICREADSTREAM_H_
