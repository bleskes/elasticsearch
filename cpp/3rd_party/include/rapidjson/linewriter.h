#ifndef RAPIDJSON_LINEWRITER_H_
#define RAPIDJSON_LINEWRITER_H_

#include "writer.h"

namespace rapidjson {

//! Writes each Json object to a single line.
//! Not as verbose as rapidjson::prettywriter but it is still possible to 
//! parse json data streamed in this format by reading one line at a time
/*!
	\tparam Stream Type of ouptut stream.
	\tparam Encoding Encoding of both source strings and output.
	\tparam Allocator Type of allocator for allocating memory of stack.
*/
template<typename Stream, typename Encoding = UTF8<>, typename Allocator = MemoryPoolAllocator<> >
class LineWriter : public Writer<Stream, Encoding, Allocator> {
public:
	typedef Writer<Stream, Encoding, Allocator> Base;
	typedef typename Base::Ch Ch;

	//! Constructor
	/*! \param stream Output stream.
		\param allocator User supplied allocator. If it is null, it will create a private one.
		\param levelDepth Initial capacity of 
	*/
	LineWriter(Stream& stream, Allocator* allocator = 0, size_t levelDepth = Base::kDefaultLevelDepth) : 
		Base(stream, allocator, levelDepth)
	{

	}

	LineWriter& StartObject() 
	{
		bool topLevel = (Base::level_stack_.GetSize() == 0);
		bool embedded = !topLevel;
		if (!topLevel)
		{
			embedded = Base::level_stack_.template Top<typename Base::Level>()->embeddedInObj ||
							(Base::level_stack_.template Top<typename Base::Level>()->inArray == false);
		}
		
		Base::Prefix(kObjectType);
		new (Base::level_stack_.template Push<typename Base::Level>()) typename Base::Level(false, embedded);
		Base::WriteStartObject();
		return *this;
	}

	LineWriter& EndObject(SizeType /*memberCount */ = 0) 
	{
		RAPIDJSON_ASSERT(Base::level_stack_.GetSize() >= sizeof(typename Base::Level));
		RAPIDJSON_ASSERT(!Base::level_stack_.template Top<typename Base::Level>()->inArray);
		
		bool isEmbedded = Base::level_stack_.template Pop<typename Base::Level>(1)->embeddedInObj;
		Base::WriteEndObject();

		if (!isEmbedded) {
			Base::stream_.Put('\n');
		}
		return *this;
	}


	LineWriter& StartArray() 
	{
		bool topLevel = Base::level_stack_.GetSize() == 0;
		bool embedded = !topLevel;
		if (!topLevel)
		{
			embedded = Base::level_stack_.template Top<typename Base::Level>()->embeddedInObj ||
						(Base::level_stack_.template Top<typename Base::Level>()->inArray == false);
		}

		Base::Prefix(kArrayType);
		new (Base::level_stack_.template Push<typename Base::Level>()) typename Base::Level(true, embedded);
		Base::WriteStartArray();
		return *this;
	}

	LineWriter& EndArray(SizeType /*elementCount*/ = 0) 
	{
		RAPIDJSON_ASSERT(Base::level_stack_.GetSize() >= sizeof(typename Base::Level));
		RAPIDJSON_ASSERT(Base::level_stack_.template Top<typename Base::Level>()->inArray);
		Base::level_stack_.template Pop<typename Base::Level>(1);
		Base::WriteEndArray();
		
		return *this;
	}



};

} // namespace rapidjson

#endif // RAPIDJSON_LINEWRITER_H_
