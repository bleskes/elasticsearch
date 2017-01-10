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
#include <maths/CBasicStatistics.h>

#include <core/CLogger.h>

#include <maths/CMathsFuncs.h>

#include <algorithm>
#include <numeric>

#include <math.h>


namespace ml
{
namespace maths
{

double CBasicStatistics::mean(const TDoubleDoublePr &samples)
{
    return 0.5 * (samples.first + samples.second);
}

double CBasicStatistics::mean(const TDoubleVec &sample)
{
    return meanPartialPreSum(0, 0.0, sample);
}

double CBasicStatistics::meanPartialPreSum(uint64_t aggregateCount,
                                           double aggregateSum,
                                           const TDoubleVec &sample)
{
    double sum(std::accumulate(sample.begin(), sample.end(), aggregateSum));

    return sum / double(aggregateCount + sample.size());
}

double CBasicStatistics::meanPartialPreCalc(uint64_t aggregateCount,
                                            double aggregateMean,
                                            const TDoubleVec &sample)
{
    double sum(std::accumulate(sample.begin(), sample.end(), 0.0));
    double totalCount(static_cast<double>(aggregateCount + sample.size()));

    return aggregateMean * (double(aggregateCount) / totalCount) +
           sum / totalCount;
}

bool CBasicStatistics::sampleStandardDeviation(
                                    const TDoubleVec &sample,
                                    double &sd)
{
    return sampleStandardDeviationPartialPreCalc(0, 0.0, 0.0, sample, sd);
}

bool CBasicStatistics::sampleStandardDeviationPartialPreSum(
                                    uint64_t aggregateCount,
                                    double aggregateSumSquares,
                                    const TDoubleVec &sample,
                                    double mean,
                                    double &sd)
{
    double var(0.0);

    if (CBasicStatistics::sampleVariancePartialPreSum(aggregateCount,
                                                      aggregateSumSquares,
                                                      sample,
                                                      mean,
                                                      var) == false)
    {
        LOG_ERROR("Unable to compute SD");
        return false;
    }

    // Defensive programming
    if (var < 0.0)
    {
        LOG_ERROR("Unable to compute SD " << var);
        return false;
    }

    sd = ::sqrt(var);

    return true;
}

bool CBasicStatistics::sampleStandardDeviationPartialPreCalc(
                                    uint64_t aggregateCount,
                                    double aggregateMean,
                                    double aggregateSumDiffMeanSquares,
                                    const TDoubleVec &sample,
                                    double &sd)
{
    double var(0.0);

    if (CBasicStatistics::sampleVariancePartialPreCalc(aggregateCount,
                                                       aggregateMean,
                                                       aggregateSumDiffMeanSquares,
                                                       sample,
                                                       var) == false)
    {
        LOG_ERROR("Unable to compute SD");
        return false;
    }

    // Defensive programming
    if (var < 0.0)
    {
        LOG_ERROR("Unable to compute SD " << var);
        return false;
    }

    sd = ::sqrt(var);

    return true;
}

bool CBasicStatistics::sampleVariance(
                                    const TDoubleVec &sample,
                                    double &var)
{
    return sampleVariancePartialPreCalc(0, 0.0, 0.0, sample, var);
}

bool CBasicStatistics::sampleVariancePartialPreSum(
                                    uint64_t aggregateCount,
                                    double aggregateSumSquares,
                                    const TDoubleVec &sample,
                                    double mean,
                                    double &var)
{
    double count(static_cast<double>(aggregateCount + sample.size()));

    if (count < 2.0)
    {
        LOG_ERROR("Cannot compute variance for sample size < 2 " << count);
        return false;
    }

    double sumSquares(aggregateSumSquares);

    for(TDoubleVecCItr itr = sample.begin(); itr != sample.end(); ++itr)
    {
        const double &temp = *itr;
        sumSquares += temp * temp;
    }

    // Compute SAMPLE variance (i.e. denominator is N-1 instead of N)
    // DANGER - this formula can suffer massive loss of precision if
    // sumSquares is very similar to (count * mean * mean).  If they
    // are both the same to 15 significant figures then the result of
    // this calculation is basically a tiny positive or negative random
    // number!  Use sampleVariancePartialPreCalc() instead if at all
    // possible.
    var = (sumSquares - count * mean * mean) / (count - 1.0);

    return true;
}

bool CBasicStatistics::sampleVariancePartialPreCalc(
                                    uint64_t aggregateCount,
                                    double aggregateMean,
                                    double aggregateSumDiffMeanSquares,
                                    const TDoubleVec &sample,
                                    double &var)
{
    double totalCount(static_cast<double>(aggregateCount + sample.size()));

    if (totalCount < 2.0)
    {
        LOG_ERROR("Cannot compute variance for sample size < 2 " << totalCount);
        return false;
    }

    // This algorithm deliberately avoids calculating a sum of squares, as
    // subtracting two huge floating point numbers that are roughly the same
    // can cause in a loss of precision, possibly resulting in wierd results,
    // e.g. negative variance.
    double sumDiffMeanSquares(aggregateSumDiffMeanSquares);
    double runningMean(aggregateMean);
    double runningCount(static_cast<double>(aggregateCount));

    for (TDoubleVecCItr itr = sample.begin(); itr != sample.end(); ++itr)
    {
        double delta(*itr - runningMean);

        runningCount += 1.0;

        runningMean += delta / runningCount;

        sumDiffMeanSquares += (delta * delta) * ((runningCount - 1.0) / runningCount);
    }

    if (totalCount != runningCount)
    {
        LOG_ERROR("Programmatic error - counts are " <<
                  totalCount << " and " << runningCount);
        return false;
    }

    // Compute SAMPLE variance (i.e. denominator is N-1 instead of N)
    var = sumDiffMeanSquares / (totalCount - 1.0);

    if (CMathsFuncs::isNan(var))
    {
        LOG_ERROR("variance is not a number :"
                  " aggregateCount = " << aggregateCount <<
                  " aggregateMean = " << aggregateMean <<
                  " aggregateSumDiffMeanSquares = " << aggregateSumDiffMeanSquares <<
                  " sample.size() = " << sample.size() <<
                  " sample[0] = " << sample.at(0) <<
                  " runningCount = " << runningCount <<
                  " runningMean = " << runningMean <<
                  " sumDiffMeanSquares = " << sumDiffMeanSquares <<
                  " var = " << var);
    }
    else
    {
        if (var < 0.0)
        {
            LOG_ERROR("negative variance calculated :"
                      " aggregateCount = " << aggregateCount <<
                      " aggregateMean = " << aggregateMean <<
                      " aggregateSumDiffMeanSquares = " << aggregateSumDiffMeanSquares <<
                      " sample.size() = " << sample.size() <<
                      " sample[0] = " << sample.at(0) <<
                      " runningCount = " << runningCount <<
                      " runningMean = " << runningMean <<
                      " sumDiffMeanSquares = " << sumDiffMeanSquares <<
                      " var = " << var);
        }
    }

    return true;
}

bool CBasicStatistics::sampleVarianceToMeanRatio(
                                    const TDoubleVec &sample,
                                    double &mean,
                                    double &vmr)
{
    double var(0.0);

    if (CBasicStatistics::sampleVariance(sample, var) == false)
    {
        vmr = 0.0;
        mean = 0.0;
        LOG_ERROR("Cannot compute variance");
        return false;
    }

    mean = CBasicStatistics::mean(sample);
    vmr = var / mean;

    return true;
}

double CBasicStatistics::median(const TDoubleVec &dataIn)
{
    if (dataIn.empty())
    {
        return 0.0;
    }

    size_t size(dataIn.size());
    if (size == 1)
    {
        return dataIn[0];
    }

    TDoubleVec data(dataIn);

    // If data size is even (1,2,3,4) then take mean of 2,3 = 2.5
    // If data size is odd (1,2,3,4,5) then take middle value = 3
    double median(0.0);

    // For an odd number of elements, this will get the median element into
    // place.  For an even number of elements, it will get the second element
    // of the middle pair into place.
    bool useMean(size % 2 == 0);
    size_t index(size / 2);
    std::nth_element(data.begin(), data.begin() + index, data.end());

    if (useMean)
    {
        // Since the nth element is the second of the two we need to average,
        // the first element to be averaged will be the largest of all those
        // before the nth one in the vector.
        TDoubleVecCItr iter = std::max_element(data.begin(), data.begin() + index);

        median = (*iter + data[index]) / 2.0;
    }
    else
    {
        median = data[index];
    }

    return median;
}

double CBasicStatistics::firstQuartile(const TDoubleVec &dataIn)
{
    if (dataIn.empty())
    {
        return 0.0;
    }

    size_t size(dataIn.size());
    if (size == 1)
    {
        return dataIn[0];
    }

    TDoubleVec data(dataIn);

    // If data size is even (1,2,3,4) then take the median of the first half of the data
    // If data size is odd (1,2,3,4,5) then take the median of the data up to and including the middle value
    double q1(0.0);

    // If the number of elements is a multiple of 4 or one less, this will get
    // the first quartile element into place.  If not, it will get the second
    // element of the middle pair into place.
    bool useMean(size % 4 == 0 || size % 4 == 3);
    size_t index((size + 1) / 4);
    std::nth_element(data.begin(), data.begin() + index, data.end());

    if (useMean)
    {
        // Since the nth element is the second of the two we need to average,
        // the first element to be averaged will be the largest of all those
        // before the nth one in the vector.
        TDoubleVecCItr iter = std::max_element(data.begin(), data.begin() + index);

        q1 = (*iter + data[index]) / 2.0;
    }
    else
    {
        q1 = data[index];
    }

    return q1;
}

double CBasicStatistics::thirdQuartile(const TDoubleVec &dataIn)
{
    if (dataIn.empty())
    {
        return 0.0;
    }

    size_t size(dataIn.size());
    if (size == 1)
    {
        return dataIn[0];
    }

    TDoubleVec data(dataIn);

    // If data size is even (1,2,3,4) then take the median of the second half of the data
    // If data size is odd (1,2,3,4,5) then take the median of the data from the middle value onwards
    double q3(0.0);

    // If the number of elements is a multiple of 4 or one less, this will get
    // the third quartile element into place.  If not, it will get the second
    // element of the middle pair into place.
    bool useMean(size % 4 == 0 || size % 4 == 3);
    size_t index(3 * size / 4);
    std::nth_element(data.begin(), data.begin() + index, data.end());

    if (useMean)
    {
        // Since the nth element is the second of the two we need to average,
        // the first element to be averaged will be the largest of all those
        // before the nth one in the vector.
        TDoubleVecCItr iter = std::max_element(data.begin(), data.begin() + index);

        q3 = (*iter + data[index]) / 2.0;
    }
    else
    {
        q3 = data[index];
    }

    return q3;
}

const char CBasicStatistics::DELIMITER(':');

}
}

