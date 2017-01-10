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
#ifndef INCLUDED_ml_test_CRandomNumbers_h
#define INCLUDED_ml_test_CRandomNumbers_h

#include <test/ImportExport.h>

#include <maths/CLinearAlgebra.h>
#include <maths/CPRNG.h>

#include <boost/bind.hpp>
#include <boost/math/constants/constants.hpp>
#include <boost/ref.hpp>
#include <boost/shared_ptr.hpp>

#include <algorithm>
#include <iterator>
#include <vector>

#include <math.h>


namespace ml
{
namespace test
{

//! \brief Creates random numbers from a variety of distributions.
class TEST_EXPORT CRandomNumbers
{
    public:
        typedef std::vector<double> TDoubleVec;
        typedef std::vector<TDoubleVec> TDoubleVecVec;
        typedef std::vector<unsigned int> TUIntVec;
        typedef std::vector<std::size_t> TSizeVec;
        typedef std::vector<std::string> TStrVec;
        typedef maths::CPRNG::CXorShift1024Mult TGenerator;
        typedef boost::shared_ptr<TGenerator> TGeneratorPtr;

    public:
        //! A uniform generator on the interval [a,b].
        class TEST_EXPORT CUniform0nGenerator
        {
            public:
                CUniform0nGenerator(const TGenerator &generator);

                std::size_t operator()(std::size_t n) const;

            private:
                TGeneratorPtr m_Generator;
        };

    public:
        //! \brief Generate random samples from the specified distribution
        //! using a custom random number generator.
        template<typename RNG,
                 typename Distribution,
                 typename Container>
        static void generateSamples(RNG &randomNumberGenerator,
                                    const Distribution &distribution,
                                    std::size_t numberSamples,
                                    Container &samples)
        {
            samples.clear();
            samples.reserve(numberSamples);
            std::generate_n(std::back_inserter(samples),
                            numberSamples,
                            boost::bind(distribution, boost::ref(randomNumberGenerator)));
        }

        //! Shuffle the elements of a sequence using a random number generator.
        //!
        //! Reorders the elements in the range \p [first,last) using the
        //! internal random number generator to provide a random distribution.
        //!
        //! \note We provide our own implementation of std::random_shuffle
        //! based on the libc++ implementation because this is different from
        //! the libstdc++ implementation which can cause platform specific test
        //! failures.
        template<typename ITR>
        void random_shuffle(ITR first, ITR last)
        {
            typedef typename std::iterator_traits<ITR>::difference_type difference_type;
            CUniform0nGenerator rand(m_Generator);
            difference_type d = last - first;
            if (d > 1)
            {
                for (--last; first < last; ++first, --d)
                {
                    difference_type i = rand(d);
                    std::iter_swap(first, first + i);
                }
            }
        }

        //! Generate normal random samples with the specified mean and
        //! variance using the default random number generator.
        void generateNormalSamples(double mean,
                                   double variance,
                                   std::size_t numberSamples,
                                   TDoubleVec &samples);

        //! Generate multivariate normal random samples with the specified
        //! mean and covariance matrix the default random number generator.
        void generateMultivariateNormalSamples(const TDoubleVec &mean,
                                               const TDoubleVecVec &covariances,
                                               std::size_t numberSamples,
                                               TDoubleVecVec &samples);

        //! Generate Poisson random samples with the specified rate using
        //! the default random number generator.
        void generatePoissonSamples(double rate,
                                    std::size_t numberSamples,
                                    TUIntVec &samples);

        //! Generate Student's t random samples with the specified degrees
        //! freedom using the default random number generator.
        void generateStudentsSamples(double degreesFreedom,
                                     std::size_t numberSamples,
                                     TDoubleVec &samples);

        //! Generate log-normal random samples with the specified location
        //! and scale using the default random number generator.
        void generateLogNormalSamples(double location,
                                      double squareScale,
                                      std::size_t numberSamples,
                                      TDoubleVec &samples);

        //! Generate uniform random samples in the interval [a,b) using
        //! the default random number generator.
        void generateUniformSamples(double a,
                                    double b,
                                    std::size_t numberSamples,
                                    TDoubleVec &samples);

        //! Generate uniform integer samples from the the set [a, a+1, ..., b)
        //! using the default random number generator.
        void generateUniformSamples(std::size_t a,
                                    std::size_t b,
                                    std::size_t numberSamples,
                                    TSizeVec &samples);

        //! Generate gamma random samples with the specified shape and rate
        //! using the default random number generator.
        void generateGammaSamples(double shape,
                                  double scale,
                                  std::size_t numberSamples,
                                  TDoubleVec &samples);

        //! Generate multinomial random samples on the specified categories
        //! using the default random number generator.
        void generateMultinomialSamples(const TDoubleVec &categories,
                                        const TDoubleVec &probabilities,
                                        std::size_t numberSamples,
                                        TDoubleVec &samples);

        //! Generate random samples from a Diriclet distribution with
        //! concentration parameters \p concentrations.
        void generateDirichletSamples(const TDoubleVec &concentrations,
                                      std::size_t numberSamples,
                                      TDoubleVecVec &samples);

        //! Generate a collection of random words of specified length using
        //! the default random number generator.
        void generateWords(std::size_t length,
                           std::size_t numberSamples,
                           TStrVec &samples);

        //! Generate a collection of |\p sizes| random mean vectors and
        //! covariance matrices and a collection of samples from those
        //! distributions.
        //!
        //! \param[in] sizes The number of points to generate from each
        //! cluster.
        //! \param[out] means Filled in with the distribution mean for
        //! each cluster.
        //! \param[out] covariances Filled in with the distribution covariance
        //! matrix for each cluster.
        //! \param[out] points Filled in with the samples from each cluster.
        template<typename T, std::size_t N>
        void generateRandomMultivariateNormals(const TSizeVec &sizes,
                                               std::vector<maths::CVectorNx1<T, N> > &means,
                                               std::vector<maths::CSymmetricMatrixNxN<T, N> > &covariances,
                                               std::vector<std::vector<maths::CVectorNx1<T, N> > > &points)
        {
            means.clear();
            covariances.clear();
            points.clear();

            std::size_t k = sizes.size();

            TDoubleVec means_;
            this->generateUniformSamples(-100.0, 100.0, N * k, means_);
            for (std::size_t i = 0; i < N * k; i += N)
            {
                maths::CVectorNx1<T, N> mean(&means_[i], &means_[i + N]);
                means.push_back(mean);
            }

            TDoubleVec variances;
            this->generateUniformSamples(10.0, 100.0, N * k, variances);
            for (std::size_t i = 0; i < k; ++i)
            {
                Eigen::Matrix<T, N, N> covariance = Eigen::Matrix<T, N, N>::Zero();

                for (std::size_t j = 0u; j < N; ++j)
                {
                    covariance(j, j) = variances[i * N + j];
                }

                // Generate random rotations in two planes.
                TSizeVec coordinates;
                this->generateUniformSamples(0, N, 4, coordinates);
                std::sort(coordinates.begin(), coordinates.end());
                coordinates.erase(std::unique(coordinates.begin(),
                                              coordinates.end()), coordinates.end());

                TDoubleVec thetas;
                this->generateUniformSamples(0.0, boost::math::constants::two_pi<double>(), 2, thetas);

                Eigen::Matrix<T, N, N> rotation = Eigen::Matrix<T, N, N>::Identity();
                for (std::size_t j = 1u; j < coordinates.size(); j += 2)
                {
                    double ct = ::cos(thetas[j/2]);
                    double st = ::sin(thetas[j/2]);

                    Eigen::Matrix<T, N, N> r = Eigen::Matrix<T, N, N>::Identity();
                    r(coordinates[j/2],   coordinates[j/2])   =  ct;
                    r(coordinates[j/2],   coordinates[j/2+1]) = -st;
                    r(coordinates[j/2+1], coordinates[j/2])   =  st;
                    r(coordinates[j/2+1], coordinates[j/2+1]) =  ct;
                    rotation *= r;
                }
                covariance = rotation.transpose() * covariance * rotation;

                covariances.push_back(maths::CSymmetricMatrixNxN<T, N>(covariance));
            }

            points.resize(k);
            TDoubleVecVec pointsi;
            for (std::size_t i = 0u; i < k; ++i)
            {
                LOG_TRACE("mean = " << means[i]);
                LOG_TRACE("covariance = " << covariances[i]);
                this->generateMultivariateNormalSamples(means[i].template toVector<TDoubleVec>(),
                                                        covariances[i].template toVectors<TDoubleVecVec>(),
                                                        sizes[i], pointsi);
                for (std::size_t j = 0u; j < pointsi.size(); ++j)
                {
                    points[i].push_back(maths::CVectorNx1<T, N>(pointsi[j]));
                }
            }
        }

        //! Get a uniform generator in the range [0, n). This can be used
        //! in conjunction with std::random_shuffle if you want a seeded
        //! platform independent implementation.
        CUniform0nGenerator uniformGenerator(void);

        //! Throw away \p n random numbers.
        void discard(std::size_t n);

    private:
        //! The random number generator.
        TGenerator m_Generator;
};


}
}

#endif // INCLUDED_ml_test_CRandomNumbers_h

