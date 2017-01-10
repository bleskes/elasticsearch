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

#include "CModelVisualiser.h"

#include <core/CoreTypes.h>
#include <core/CTimeUtils.h>

#include <maths/CPrior.h>

#include "CContainerOctavePrinter.h"
#include "CVanillaFileReader.h"

#include <boost/scoped_ptr.hpp>

#include <fstream>
#include <iostream>
#include <utility>
#include <vector>


namespace ml
{
namespace model_visualiser
{

namespace
{
const std::size_t NUMBER_SAMPLES = 100u;
//const char *RESULTS_FILE_NAME = "results2";
typedef std::vector<double> TDoubleVec;
typedef std::pair<double, double> TDoubleDoublePr;
typedef std::vector<TDoubleDoublePr> TDoubleDoublePrVec;

}

CModelVisualiser::CModelVisualiser(model::CLimits &limits,
                                   const api::CFieldConfig &fieldConfig,
                                   const model::CModelConfig &modelConfig,
                                   api::COutputHandler &outputHandler) :
        api::CAnomalyDetector("n/a",
                              limits,
                              fieldConfig,
                              modelConfig,
                              outputHandler)
{
}

bool CModelVisualiser::load(const std::string &filename)
{
    core_t::TTime completeToTime = 0;
    model_visualiser::CVanillaFileReader reader(filename);
    return this->restoreState(reader, completeToTime)
           || completeToTime == 0;
}

void CModelVisualiser::visualise(core_t::TTime time,
                                 const std::string &byClause,
                                 const std::string &overClause) const
{
    api::CAnomalyDetector::TAnomalyDetectorPtrVec detectors;
    this->detectors(detectors);

    // TODO need to think about what to do about detector key.

    for (std::size_t i = 0u; i < detectors.size(); ++i)
    {
        const model::CModelEnsemble &models =
                model::CModelView(*detectors[i]).models();

        const model::CModel *model = models.model(time);
        if (!model)
        {
            LOG_ERROR("No models at " << core::CTimeUtils::toIso8601(time));
            continue;
        }

        typedef boost::scoped_ptr<const model::CModelDetailsView> TScopedModelDetailsViewPtr;
        TScopedModelDetailsViewPtr details(model->details());

        if (!model->isPopulation())
        {
            if (overClause != "")
            {
                LOG_ERROR("Over clause field value supplied but model isn't population");
            }

            const model::CModelDetailsView::TFeatureVec &features = details->features();

            std::size_t pid;
            if (!details->personId(byClause, pid))
            {
                LOG_ERROR("Person \"" << byClause << "\" not found");
                continue;
            }

            for (std::size_t j = 0u; j < features.size(); ++j)
            {
                const maths::CPrior *prior = details->personPrior(features[j], pid, 0);
                if (!prior)
                {
                    LOG_ERROR("No prior for \"" << byClause
                              << "\" and " << model_t::print(features[j]));
                    continue;
                }

                TDoubleVec xx;

                prior->sampleMarginalLikelihood(NUMBER_SAMPLES, xx);
                std::pair<double, double> confidenceInterval = prior->marginalLikelihoodConfidenceInterval(99.99);
                xx.push_back(confidenceInterval.first);
                xx.push_back(confidenceInterval.second);
                std::sort(xx.begin(),xx.end());
                TDoubleDoublePrVec x(1, TDoubleDoublePr(0.0, 1.0));
                TDoubleVec ff;
                ff.reserve(xx.size());
                for (std::size_t k = 0u, l = 0u; k < xx.size(); ++k)
                {
                    x[0].first = xx[k];
                    double f;
                    if (prior->jointLogMarginalLikelihood(maths_t::E_SampleCountWeight, x, f)
                            == maths_t::E_FpNoErrors)
                    {
                        ff.push_back(::exp(f));
                        xx[l] = xx[k];
                        ++l;
                    }
                }
                xx.erase(xx.begin() + ff.size(), xx.end());

                //maths::COrderings::simultaneousSort(xx,ff);
                //std::ofstream results;
                //results.open(RESULTS_FILE_NAME, std::ios_base::app);
                //results << model_t::print(features[j]) << "\n";
                std::cout << "# name: <cell-element>\n";
                std::cout << "# type: matrix\n";
                std::cout << "# rows: 1\n";
                std::cout << "# columns: " << xx.size() << "\n";
                std::cout << "" << model_visualiser::CContainerOctavePrinter::printForOctave(xx) << "\n\n";
                std::cout << "# name: <cell-element>\n";
                std::cout << "# type: matrix\n";
                std::cout << "# rows: 1\n";
                std::cout << "# columns: " << ff.size() << "\n";
                std::cout << "" << model_visualiser::CContainerOctavePrinter::printForOctave(ff) << "\n\n\n";
            }
        }
        else // Is population
        {
            const model::CModelDetailsView::TFeatureVec &features = details->features();

            std::size_t cid = 0u;
            if (!byClause.empty()
                && !details->categoryId(byClause, cid))
            {
                LOG_ERROR("Attribute \"" << byClause << "\" not found");
                continue;
            }

            // We don't currently create person models so don't
            // bother to extract the person identifier here.

            for (std::size_t j = 0u; j < features.size(); ++j)
            {
                const maths::CPrior *prior = details->prior(features[j], cid);
                if (!prior)
                {
                    LOG_ERROR("No prior for \"" << byClause
                              << "\" and " << model_t::print(features[j]));
                    continue;
                }

                TDoubleVec xx;
                prior->sampleMarginalLikelihood(NUMBER_SAMPLES, xx);

                std::pair<double, double> confidenceInterval = prior->marginalLikelihoodConfidenceInterval(99.99);
                xx.push_back(confidenceInterval.first);
                xx.push_back(confidenceInterval.second);
                std::sort(xx.begin(),xx.end());
                TDoubleDoublePrVec x(1, TDoubleDoublePr(0.0, 1.0));
                TDoubleVec ff;
                ff.reserve(xx.size());
                for (std::size_t k = 0u, l = 0u; k < xx.size(); ++k)
                {
                    x[0].first = xx[k];
                    double f;
                    if (prior->jointLogMarginalLikelihood(maths_t::E_SampleCountWeight, x, f)
                            == maths_t::E_FpNoErrors)
                    {
                        ff.push_back(::exp(f));
                        xx[l] = xx[k];
                        ++l;
                    }
                }
                xx.erase(xx.begin() + ff.size(), xx.end());
                //maths::COrderings::simultaneousSort(xx,ff);
                //std::ofstream results;
                //results.open(RESULTS_FILE_NAME, std::ios_base::app);
                //results << model_t::print(features[j]) << "\n";
                std::cout << "# name: <cell-element>\n";
                std::cout << "# type: matrix\n";
                std::cout << "# rows: 1\n";
                std::cout << "# columns: " << xx.size() << "\n";
                std::cout << "" << model_visualiser::CContainerOctavePrinter::printForOctave(xx) << "\n\n";
                std::cout << "# name: <cell-element>\n";
                std::cout << "# type: matrix\n";
                std::cout << "# rows: 1\n";
                std::cout << "# columns: " << ff.size() << "\n";
                std::cout << "" << model_visualiser::CContainerOctavePrinter::printForOctave(ff) << "\n\n\n";
            }
        }
    }
}
}
}
