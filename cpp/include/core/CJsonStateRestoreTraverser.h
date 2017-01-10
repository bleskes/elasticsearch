/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
#ifndef INCLUDED_ml_core_CJsonStateRestoreTraverser_h
#define INCLUDED_ml_core_CJsonStateRestoreTraverser_h

#include <core/CNonCopyable.h>
#include <core/CStateRestoreTraverser.h>
#include <core/ImportExport.h>

#include <rapidjson/GenericReadStream.h>
#include <rapidjson/pullreader.h>

#include <iosfwd>


namespace ml
{
namespace core
{


//! \brief
//! For restoring state in JSON format.
//!
//! DESCRIPTION:\n
//! Concrete implementation of the CStateRestoreTraverser interface
//! that restores state in JSON format.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Input is streaming rather than building up an in-memory JSON
//! document.
//!
//! Unlike the CRapidXmlStatePersistInserter, there is no possibility
//! of including attributes on the root node (because JSON does not
//! have attributes).  This may complicate code that needs to be 100%
//! JSON/XML agnostic.
//!
class CORE_EXPORT CJsonStateRestoreTraverser : public CStateRestoreTraverser
{
    public:
        CJsonStateRestoreTraverser(std::istream &inputStream);

        //! Navigate to the next element at the current level, or return false
        //! if there isn't one
        virtual bool next(void);

        //! Go to the start of the next object
        //! Stops at the first '}' character so this will not
        //! work with nested objects
        bool nextObject(void);

        //! Does the current element have a sub-level?
        virtual bool hasSubLevel(void) const;

        //! Get the name of the current element - the returned reference is only
        //! valid for as long as the traverser is pointing at the same element
        virtual const std::string &name(void) const;

        //! Get the value of the current element - the returned reference is
        //! only valid for as long as the traverser is pointing at the same
        //! element
        virtual const std::string &value(void) const;

        //! Is the traverser at the end of the inputstream?
        virtual bool isEof(void) const;

    protected:
        //! Navigate to the start of the sub-level of the current element, or
        //! return false if there isn't one
        virtual bool descend(void);

        //! Navigate to the element of the level above from which descend() was
        //! called, or return false if there isn't a level above
        virtual bool ascend(void);

        //! Print debug
        void debug(void) const;

    private:
        //! Accessors for alternating state variables
        size_t currentLevel(void) const;
        bool currentIsEndOfLevel(void) const;
        const std::string &currentName(void) const;
        const std::string &currentValue(void) const;
        size_t nextLevel(void) const;
        bool nextIsEndOfLevel(void) const;
        const std::string &nextName(void) const;
        const std::string &nextValue(void) const;

        //! Start off the parsing process
        bool start(void);

        //! Get the next token
        //! \param stopAtStartOfObject Continue till the next object start is found.
        //! If an nested object is encounted it will stop there
        bool advance(bool stopAtStartOfObject = false);

        //! Log an error that the JSON parser has detected
        void logError(void);

    private:
        //! JSON reader istream wrapper
        rapidjson::GenericReadStream m_ReadStream;

        //! JSON reader
        rapidjson::PullReader        m_Reader;

        //! Flag to indicate whether we've started parsing
        bool                         m_Started;

        //! Flag to indicate that we expect to parse a name
        bool                         m_ExpectName;

        //! Which level within the JSON structure do we want to be getting
        //! values from?
        size_t                       m_DesiredLevel;

        //! We need to know the current element plus what's coming next.
        //! To avoid lots of copying these are stored in arrays where
        //! one element represents the current element and the other
        //! element the next element.  The information for the current
        //! element is stored at array index (1 - m_NextIndex) and the
        //! index of the next element is stored at array index
        //! m_NextIndex.
        size_t                       m_Level[2];
        bool                         m_IsEndOfLevel[2];
        std::string                  m_Name[2];
        std::string                  m_Value[2];

        //! Setting m_NextIndex = (1 - m_NextIndex) advances the
        //! stored details.
        size_t                       m_NextIndex;

        //! If the first token is an '[' then we are parsing an array of objects
        bool                         m_IsArrayOfObjects;
};


}
}

#endif // INCLUDED_ml_core_CJsonStateRestoreTraverser_h

