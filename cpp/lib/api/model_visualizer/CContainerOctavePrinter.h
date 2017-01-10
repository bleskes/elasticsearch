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

#ifndef INCLUDED_ml_core_CContainerOctavePrinter_h
#define INCLUDED_ml_core_CContainerOctavePrinter_h

#include <core/CStringUtils.h>
#include <core/ImportExport.h>
#include <core/CContainerPrinter.h>

#include <boost/ref.hpp>

#include <sstream>
//#include <string>

namespace ml
{
namespace model_visualiser
{

class CORE_EXPORT CContainerOctavePrinter : core::CContainerPrinter
{
    public:
        //! Function object wrapper around printElement for use with STL.
        class CElementPrinter
        {
            public:
                template<typename T>
                std::string operator()(const T &value)
                {
                    return printElement(value);
                }
        };

        //! Print a range of values as defined by a start and end iterator
        //! for debug. This assumes that ITR is a forward iterator, i.e.
        //! it must implement prefix ++ and * operators.
        template<typename ITR>
        static std::string printForOctave(ITR begin, ITR end)
        {
            std::ostringstream result;

            result << " ";
            if (begin != end)
            {
                for (;;)
                {
                    result << printElement(boost::unwrap_ref(*begin));
                    if (++begin == end)
                    {
                        break;
                    }
                    result << " ";
                }
            }
            result << " ";

            return result.str();
        }

        //! Print a STL compliant container for debug.
        template<typename CONTAINER>
        static std::string printForOctave(const CONTAINER &container)
        {
            return printForOctave(boost::unwrap_ref(container).begin(),
                                  boost::unwrap_ref(container).end());
        }

        //! Annoyingly boost::unwrap_ref fails on the result
        //! of dereferencing a vector<bool> iterator so provide
        //! explicit overload.
        static std::string printForOctave(const std::vector<bool> &bits)
        {
            if (bits.empty())
            {
                return "  ";
            }

            std::ostringstream result;
            result << " " << core::CStringUtils::typeToStringPretty(bits[0]);
            for (std::size_t i = 1u; i < bits.size(); ++i)
            {
                result << " " << core::CStringUtils::typeToStringPretty(bits[i]);
            }
            result << " ";
            return result.str();
        }

        //! Specialization for arrays.
        template<typename T, std::size_t SIZE>
        static std::string printForOctave(const T (&array)[SIZE])
        {
            return printForOctave(array, array + SIZE);
        }
};

}
}


#endif // INCLUDED_ml_core_CContainerOctavePrinter_h
