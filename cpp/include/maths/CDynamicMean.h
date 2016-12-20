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
#ifndef INCLUDED_prelert_maths_CDynamicMean_h
#define INCLUDED_prelert_maths_CDynamicMean_h

#include <core/CLogger.h>

#include <stdint.h>


namespace prelert
{
namespace maths
{


//! \brief
//! Calculation of mean and variance of a sliding windowed stream.
//!
//! DESCRIPTION:\n
//! Calculation of mean and variance of a sliding windowed stream.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Implementation assumes that the values are approx. < 1000
//! as sums of squares are stored and rounding errors are avoided.
//!
//! Naively, the mean and variance can be computed by:
/*
n = 0
sum = 0
sum_sqr = 0

for x in data:
    n = n + 1
    sum = sum + x
    sum_sqr = sum_sqr + x*x

mean = sum/n
variance = (sum_sqr - sum*mean)/(n - 1)
*/
//! Unfortunately, if the standard deviaton is small relative to the mean
//! this method is inaccurate due to the inherent precision of a double.
//!
//! A better algorithm is from Knuth reduces this rounding errors:
/*
n = 0
mean = 0
M2 = 0

for x in data:
    n = n + 1
    delta = x - mean
    mean = mean + delta/n
    M2 = M2 + delta*(x - mean)  # This expression uses the new value of mean

variance_n = M2/n
variance = M2/(n - 1)
*/
//! Unfortunately, for a stream this algorithm can not easily remove the last
//! element.
//!
//! Therefore, the 'naive' method is used so a constant sum of values
//! and their squares are used under the assumption that the
//! values here are small (<1000).
//!

template <typename T>
class CDynamicMean
{
    public:
        CDynamicMean(void)
            : m_N(0),
              m_Sum(0),
              m_SumSquares(0),
              m_Mean(0),
              m_Variance(0)
        {
        }

        //! Add a value and dynamically calculate the updated mean and variance
        void newData(T x)
        {
            double mean;
            double var;

            // TODO not optimal as mean is calculated anyway
            this->newData(x, mean, var);
        }

        void newData(T x, double &mean, double &variance)
        {
            // When T is unsigned int, this comparison will always return false
            if (x < 0)
            {
                LOG_ERROR("All data MUST be +ve " << x);
                return;
            }

            ++m_N;

            double doubleX(static_cast<double>(x));
            m_Sum += doubleX;
            m_SumSquares += doubleX * doubleX;

            m_Mean = m_Sum / double(m_N);

            if (m_N > 1)
            {
                // Use N - 1 for the naive approach
                m_Variance = (m_SumSquares - m_Sum * m_Mean) / double(m_N - 1);
            }

            mean = m_Mean;
            variance = m_Variance;
        }

        //! Delete an added value and dynamically calculate the updated mean and variance
        void delData(T x)
        {
            double mean;
            double var;

            // TODO not optimal as mean is calculated anyway
            this->delData(x, mean, var);
        }

        void delData(T x, double &mean, double &variance)
        {
            if (m_N < 1)
            {
                LOG_ERROR("Inconsistency");
                return;
            }

            if (--m_N == 0)
            {
                m_Sum = 0.0;
                m_SumSquares = 0.0;
                m_Mean = 0.0;
                m_Variance = 0.0;

                return;
            }

            double doubleX(static_cast<double>(x));
            m_Sum -= doubleX;
            m_SumSquares -= doubleX * doubleX;

            m_Mean = m_Sum / double(m_N);

            if (m_N > 1)
            {
                // Use N - 1 for the naive approach
                m_Variance = (m_SumSquares - m_Sum * m_Mean) / double(m_N - 1);
            }
            else
            {
                m_Variance = 0.0;
            }

            mean = m_Mean;
            variance = m_Variance;
        }

        uint64_t N(void) const
        {
            return m_N;
        }

        double mean(void) const
        {
            return m_Mean;
        }

        double variance(void) const
        {
            return m_Variance;
        }

    private:
        //! Use notation from Knuth
        uint64_t m_N;
        double   m_Sum;
        double   m_SumSquares;
        double   m_Mean;
        double   m_Variance;
};


}
}

#endif // INCLUDED_prelert_maths_CDynamicMean_h

