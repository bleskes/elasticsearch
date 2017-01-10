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

#include "CTimeSeriesDecompositionTest.h"

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CRapidXmlParser.h>
#include <core/CRapidXmlStatePersistInserter.h>
#include <core/CRapidXmlStateRestoreTraverser.h>
#include <core/CRegex.h>

#include <maths/CIntegerTools.h>
#include <maths/CNormalMeanPrecConjugate.h>
#include <maths/CTimeSeriesDecomposition.h>
#include <maths/CTimeSeriesTestData.h>

#include <test/CRandomNumbers.h>

#include <boost/math/constants/constants.hpp>
#include <boost/range.hpp>

#include <fstream>

using namespace ml;

namespace
{

typedef std::pair<double, double> TDoubleDoublePr;
typedef std::vector<double> TDoubleVec;
typedef std::vector<TDoubleVec> TDoubleVecVec;
typedef std::vector<core_t::TTime> TTimeVec;
typedef std::pair<core_t::TTime, double> TTimeDoublePr;
typedef std::vector<TTimeDoublePr> TTimeDoublePrVec;
typedef maths::CTimeSeriesDecomposition::TComponentVec TComponentVec;

double mean(const TDoubleDoublePr &x)
{
    return (x.first + x.second) / 2.0;
}

}

void CTimeSeriesDecompositionTest::testSuperpositionOfSines(void)
{
    LOG_DEBUG("+----------------------------------------------------------+");
    LOG_DEBUG("|  CTimeSeriesDecompositionTest::testSuperpositionOfSines  |");
    LOG_DEBUG("+----------------------------------------------------------+");

    const core_t::TTime halfHour = 1800;
    const core_t::TTime day = 86400;
    const core_t::TTime week = 604800;

    TTimeVec times;
    TDoubleVec timeseries;
    for (core_t::TTime time = 0; time < 100 * week + 1; time += halfHour)
    {
        double weekly = 1200.0 + 1000.0 * ::sin(boost::math::double_constants::two_pi
                                                * static_cast<double>(time)
                                                / static_cast<double>(week));
        double daily = 5.0 + 5.0 * ::sin(boost::math::double_constants::two_pi
                                         * static_cast<double>(time)
                                         / static_cast<double>(day));
        times.push_back(time);
        timeseries.push_back(weekly * daily);
    }

    test::CRandomNumbers rng;
    TDoubleVec noise;
    rng.generateNormalSamples(0.0, 400.0, times.size(), noise);

    core_t::TTime lastWeek = 0;
    maths::CTimeSeriesDecomposition decomposition(0.01);

    double totalSumResidual = 0.0;
    double totalMaxResidual = 0.0;
    double totalSumValue = 0.0;
    double totalMaxValue = 0.0;
    double totalPercentileError = 0.0;

    //std::ofstream file;
    //file.open("results.m");
    //file << "hold on;\n";
    //file << "t = " << core::CContainerPrinter::print(times) << ";\n";
    //file << "f = " << core::CContainerPrinter::print(timeseries) << ";\n";
    //file << "plot(t, f);\n";
    //TDoubleVec f;
    //TDoubleVec r;

    for (std::size_t i = 0u; i < times.size(); ++i)
    {
        core_t::TTime time = times[i];
        double value = timeseries[i] + noise[i];
        decomposition.addPoint(time, value);

        if (time >= lastWeek + week)
        {
            LOG_DEBUG("Processing week");

            double sumResidual = 0.0;
            double maxResidual = 0.0;
            double sumValue = 0.0;
            double maxValue = 0.0;
            double percentileError = 0.0;

            for (core_t::TTime t = lastWeek; t < lastWeek + week; t += halfHour)
            {
                TDoubleDoublePr baseline = decomposition.baseline(t + week, 70.0);
                double residual = ::fabs(timeseries[t / halfHour] - mean(baseline));
                sumResidual += residual;
                maxResidual = std::max(maxResidual, residual);
                sumValue += ::fabs(timeseries[t / halfHour]);
                maxValue = std::max(maxValue, ::fabs(timeseries[t / halfHour]));
                percentileError += std::max(std::max(baseline.first - timeseries[t / halfHour],
                                                     timeseries[t / halfHour] - baseline.second), 0.0);

                //f.push_back(mean(baseline));
                //r.push_back(mean(baseline) - timeseries[t / halfHour]);
            }

            LOG_DEBUG("'sum residual' / 'sum value' = " << sumResidual / sumValue);
            LOG_DEBUG("'max residual' / 'max value' = " << maxResidual / maxValue);
            LOG_DEBUG("70% error = " << percentileError / sumValue);

            if (time >= 2 * week)
            {
                CPPUNIT_ASSERT(sumResidual < 0.065 * sumValue);
                CPPUNIT_ASSERT(maxResidual < 0.090 * maxValue);
                CPPUNIT_ASSERT(percentileError < 0.051 * sumValue);

                totalSumResidual += sumResidual;
                totalMaxResidual += maxResidual;
                totalSumValue += sumValue;
                totalMaxValue += maxValue;
                totalPercentileError += percentileError;

                decomposition.propagateForwardsTo(time);
            }

            lastWeek += week;
        }
    }

    LOG_DEBUG("total 'sum residual' / 'sum value' = " << totalSumResidual / totalSumValue);
    LOG_DEBUG("total 'max residual' / 'max value' = " << totalMaxResidual / totalMaxValue);
    LOG_DEBUG("total 70% error = " << totalPercentileError / totalSumValue);

    CPPUNIT_ASSERT(totalSumResidual < 0.041 * totalSumValue);
    CPPUNIT_ASSERT(totalMaxResidual < 0.057 * totalMaxValue);
    CPPUNIT_ASSERT(totalPercentileError < 0.026 * totalSumValue);

    //file << "fe = " << core::CContainerPrinter::print(f) << ";\n";
    //file << "r = " << core::CContainerPrinter::print(r) << ";\n";
    //file << "plot(t(1:length(fe)), fe, 'r');\n";
    //file << "scatter(t(1:length(r)), r, 15, 'k', 'x');\n";
}

void CTimeSeriesDecompositionTest::testBlueCoat(void)
{
    LOG_DEBUG("+----------------------------------------------+");
    LOG_DEBUG("|  CTimeSeriesDecompositionTest::testBlueCoat  |");
    LOG_DEBUG("+----------------------------------------------+");

    const core_t::TTime bucketSpan = 3600;
    const core_t::TTime startTime = 0;
    const double timeseries[] =
        {
            323444,  960510,  880176,  844190,  823993,  814251,  857187,  856791,  862060,  919632,
            1083704, 2904437, 4601750, 5447896, 5827498, 5924161, 5851895, 5768661, 5927840, 5326236,
            4037245, 1958521, 1360753, 1005194, 901930,  856605,  838370,  810396,  776815,  751163,
            793055,  823974,  820458,  840647,  878594,  1192154, 2321550, 2646460, 2760957, 2838611,
            2784696, 2798327, 2643123, 2028970, 1331199, 1098105, 930971,  907562,  903603,  873554,
            879375,  852853,  828554,  819726,  872418,  856365,  860880,  867119,  873912,  885405,
            1053530, 1487664, 1555301, 1637137, 1672030, 1659346, 1514673, 1228543, 1011740, 928749,
            809702,  838931,  847904,  829188,  822558,  798517,  767446,  750486,  783165,  815612,
            825365,  873486,  1165250, 2977382, 4868975, 6050263, 6470794, 6271899, 6449326, 6352992,
            6162712, 6257295, 4570133, 1781374, 1182546, 665858,  522585,  481588,  395139,  380770,
            379182,  356068,  353498,  347707,  350931,  417253,  989129,  2884728, 4640841, 5423474,
            6246182, 6432793, 6338419, 6312346, 6294323, 6102676, 4505021, 2168289, 1411233, 1055797,
            954338,  918498,  904236,  870193,  843259,  682538,  895407,  883550,  897026,  918838,
            1262303, 3208919, 5193013, 5787263, 6255837, 6337684, 6335017, 6278740, 6191046, 6183259,
            4455055, 2004058, 1425910, 1069949, 942839,  899157,  895133,  858268,  837338,  820983,
            870863,  871873,  881182,  918795,  1237336, 3069272, 4708229, 5672066, 6291124, 6407806,
            6479889, 6533138, 3473382, 6534838, 4800911, 2668073, 1644350, 1282450, 1131734, 1009042,
            891099,  857339,  842849,  816513,  879200,  848292,  858014,  906642,  1208147, 2964568,
            5215885, 5777105, 6332104, 6130733, 6284960, 6157055, 6165520, 5771121, 4309930, 2150044,
            1475275, 1065030, 967267,  890413,  887174,  835741,  814749,  817443,  853085,  851040,
            866029,  867612,  917833,  1225383, 2326451, 2837337, 2975288, 3034415, 3056379, 3181951,
            2938511, 2400202, 1444952, 1058781, 845703,  810419,  805781,  789438,  799674,  775703,
            756145,  727587,  756489,  789886,  784948,  788247,  802013,  832272,  845033,  873396,
            1018788, 1013089, 1095001, 1022910, 798183,  519186,  320507,  247320,  139372,  129477,
            145576,  122348,  120286,  89370,   95583,   88985,   89009,   97425,   103628,  153229,
            675828,  2807240, 4652249, 5170466, 5642965, 5608709, 5697374, 5546758, 5368913, 5161602,
            3793675, 1375703, 593920,  340764,  197075,  174981,  158274,  130148,  125235,  122526,
            113896,  116249,  126881,  213814,  816723,  2690434, 4827493, 5723621, 6219650, 6492638,
            6570160, 6493706, 6495303, 6301872, 4300612, 1543551, 785562,  390012,  234939,  202190,
            142855,  135218,  124238,  111981,  104807,  107687,  129438,  190294,  779698,  2864053,
            5079395, 5912629, 6481437, 6284107, 6451007, 6177724, 5993932, 6075918, 4140658, 1481179,
            682711,  328387,  233915,  182721,  170860,  139540,  137613,  121669,  116906,  121780,
            127887,  199762,  783099,  2890355, 4658524, 5535842, 6117719, 6322938, 6570422, 6396874,
            6586615, 6332100, 4715160, 2604366, 1525620, 906137,  499019,  358856,  225543,  171388,
            153826,  149910,  141092,  136459,  161202,  240704,  766755,  3011958, 5024254, 5901640,
            6244757, 6257553, 6380236, 6394732, 6385424, 5876960, 4182127, 1868461, 883771,  377159,
            264435,  196674,  181845,  138307,  136055,  133143,  129791,  133694,  127502,  136351,
            212305,  777873,  2219051, 2732315, 2965287, 2895288, 2829988, 2818268, 2513817, 1866217,
            985099,  561287,  205195,  173997,  166428,  165294,  130072,  113917,  113282,  112466,
            103406,  115687,  159863,  158310,  225454,  516925,  1268760, 1523357, 1607510, 1560200,
            1483823, 1401526, 999236,  495292,  299905,  286900,  209697,  169881,  157560,  139030,
            132342,  187941,  126162,  106587,  108759,  109495,  116386,  208504,  676794,  1549362,
            2080332, 2488707, 2699237, 2862970, 2602994, 2554047, 2364456, 1997686, 1192434, 891293,
            697769,  391385,  234311,  231839,  160520,  155870,  142220,  139360,  142885,  141589,
            166792,  443202,  2019645, 4558828, 5982111, 6408009, 6514598, 6567566, 6686935, 6532886,
            6473927, 5475257, 2889913, 1524673, 938262,  557410,  325965,  186484,  174831,  211765,
            145477,  148318,  130425,  136431,  182002,  442272,  2078908, 4628945, 5767034, 6212302,
            6566196, 6527687, 6365204, 6226173, 6401203, 5629733, 3004625, 1555528, 1025549, 492910,
            347948,  298725,  272955,  238279,  209290,  188551,  175447,  173960,  190875,  468340,
            1885268, 4133457, 5350137, 5885807, 6331254, 6420279, 6589448, 6483637, 6557769, 5543938,
            3482732, 2010293, 1278681, 735111,  406042,  283694,  181213,  160207,  136347,  113484,
            118521,  127725,  151408,  396552,  1900747, 4400918, 5546984, 6213423, 6464686, 6442904,
            6385002, 6248314, 5880523, 4816342, 2597450, 1374071, 751391,  362615,  215644,  175158,
            116896,  127935,  110407,  113054,  105841,  113717,  177240,  206515,  616005,  1718878,
            2391747, 2450915, 2653897, 2922320, 2808467, 2490078, 1829760, 1219997, 643936,  400743,
            208976,  119623,  110170,  99338,   93661,   100187,  90803,   83980,   75950,   78805,
            95664,   108467,  128293,  294080,  720811,  965705,  1048021, 1125912, 1194746, 1114704,
            799721,  512542,  353694,  291046,  229723,  206109,  183482,  192225,  191906,  176942,
            148163,  145405,  145728,  159016,  181991,  436297,  1983374, 4688246, 5853284, 6243628,
            6730707, 6660743, 6476024, 6422004, 6335113, 5386230, 2761698, 1230646, 763506,  359071,
            223956,  189020,  158090,  145730,  135338,  114941,  108313,  120023,  167161,  440103,
            1781778, 4428615, 5701824, 6296598, 6541586, 6809286, 6716690, 6488941, 6567385, 5633685,
            2760255, 1316495, 732572,  316496,  225013,  202664,  171295,  143195,  123555,  125327,
            123357,  135419,  194933,  428197,  2181096, 4672692, 5854393, 6553263, 6653127, 6772664,
            6899086, 6794041, 6900871, 6087645, 2814928, 1393906, 894417,  413459,  280839,  237468,
            184947,  214658,  180059,  145215,  134793,  133423,  191388,  417885,  2081899, 4836758,
            5803495, 6451696, 7270708, 7628500, 7208066, 7403079, 7548585, 6323024, 3763029, 2197174,
            1359687, 857604,  471729,  338888,  177156,  150619,  145775,  132845,  110888,  121863,
            141321,  440528,  2020529, 4615833, 5772372, 6318037, 6481658, 6454979, 6489447, 6558612,
            6114653, 5009113, 2541519, 1329520, 663124,  311088,  200332,  141768,  120845,  120603,
            114688,  111340,  95757,   91444,   103287,  130905,  551108,  1988083, 2885196, 2962413,
            3070689, 3061746, 2999362, 2993871, 2287683, 1539262, 763592,  393769,  193094,  126535,
            131721,  125761,  105550,  89077,   90295,   93853,   84496,   77731,   89389,   101269,
            153379,  443022,  1114121, 1556021, 1607693, 1589743, 1746231, 1432261, 1022052
        };


    const core_t::TTime hour = 3600;
    const core_t::TTime week = 604800;

    core_t::TTime time = startTime;
    core_t::TTime lastWeek = startTime;
    maths::CTimeSeriesDecomposition decomposition(0.01);

    //std::ofstream file;
    //file.open("results.m");
    //file << "hold on;\n";

    double totalSumResidual = 0.0;
    double totalMaxResidual = 0.0;
    double totalSumValue = 0.0;
    double totalMaxValue = 0.0;
    double totalPercentileError = 0.0;

    for (std::size_t i = 0u; i < boost::size(timeseries); ++i, time += bucketSpan)
    {
        decomposition.addPoint(time, timeseries[i]);

        if (time >= lastWeek + week || i == boost::size(timeseries) - 1)
        {
            LOG_DEBUG("Processing week");

            //TDoubleVec t;
            //TDoubleVec f;
            //TDoubleVec fe;
            //TDoubleVec r;

            double sumResidual = 0.0;
            double maxResidual = 0.0;
            double sumValue = 0.0;
            double maxValue = 0.0;
            double percentileError = 0.0;

            for (core_t::TTime tt = lastWeek;
                 tt < lastWeek + week &&
                 static_cast<std::size_t>(tt / hour) < boost::size(timeseries);
                 tt += hour)
            {
                TDoubleDoublePr baseline = decomposition.baseline(tt + week, 70.0);

                double residual = ::fabs(timeseries[tt / hour] - mean(baseline));
                sumResidual += residual;
                maxResidual = std::max(maxResidual, residual);
                sumValue += ::fabs(timeseries[tt / hour]);
                maxValue = std::max(maxValue, ::fabs(timeseries[tt / hour]));
                percentileError += std::max(std::max(baseline.first - timeseries[tt / hour],
                                                     timeseries[tt / hour] - baseline.second), 0.0);

                //t.push_back(tt);
                //f.push_back(timeseries[tt / hour]);
                //fe.push_back(mean(baseline));
                //r.push_back(mean(baseline) - timeseries[tt / hour]);
            }

            LOG_DEBUG("'sum residual' / 'sum value' = " << sumResidual / sumValue);
            LOG_DEBUG("'max residual' / 'max value' = " << maxResidual / maxValue);
            LOG_DEBUG("70% error = " << percentileError / sumValue);

            if (time >= 2 * week)
            {
                CPPUNIT_ASSERT(sumResidual < 0.21 * sumValue);
                CPPUNIT_ASSERT(maxResidual < 0.54 * maxValue);
                CPPUNIT_ASSERT(percentileError < 0.08 * sumValue);

                totalSumResidual += sumResidual;
                totalMaxResidual += maxResidual;
                totalSumValue += sumValue;
                totalMaxValue += maxValue;
                totalPercentileError += percentileError;
            }

            //file << "t = " << core::CContainerPrinter::print(t) << ";\n";
            //file << "f = " << core::CContainerPrinter::print(f) << ";\n";
            //file << "fe = " << core::CContainerPrinter::print(fe) << ";\n";
            //file << "r = " << core::CContainerPrinter::print(r) << ";\n";
            //file << "plot(t, f);\n";
            //file << "plot(t, fe, 'r');\n";
            //file << "scatter(t, r, 15, 'k', 'x');\n";

            decomposition.propagateForwardsTo(time);
            lastWeek += week;
        }
    }

    LOG_DEBUG("total 'sum residual' / 'sum value' = " << totalSumResidual / totalSumValue);
    LOG_DEBUG("total 'max residual' / 'max value' = " << totalMaxResidual / totalMaxValue);
    LOG_DEBUG("total 70% error = " << totalPercentileError / totalSumValue);

    CPPUNIT_ASSERT(totalSumResidual < 0.16 * totalSumValue);
    CPPUNIT_ASSERT(totalMaxResidual < 0.28 * totalMaxValue);
    CPPUNIT_ASSERT(totalPercentileError < 0.06 * totalSumValue);
}

void CTimeSeriesDecompositionTest::testMinimizeSlope(void)
{
    LOG_DEBUG("+---------------------------------------------------+");
    LOG_DEBUG("|  CTimeSeriesDecompositionTest::testMinimizeSlope  |");
    LOG_DEBUG("+---------------------------------------------------+");

    const core_t::TTime halfHour = 1800;
    const core_t::TTime day = 86400;
    const core_t::TTime week = 604800;

    double weights[] = { 1.0, 0.1, 1.0, 1.0, 0.1, 1.0, 1.0 };

    TTimeVec times;
    TDoubleVec timeseries;
    for (core_t::TTime time = 0; time < 100 * week; time += halfHour)
    {
        double weight = weights[(time / day) % 7];
        double daily = 100.0 * ::sin(boost::math::double_constants::two_pi
                                     * static_cast<double>(time)
                                     / static_cast<double>(day));
        times.push_back(time);
        timeseries.push_back(weight * daily);
    }

    test::CRandomNumbers rng;
    TDoubleVec noise;
    rng.generateNormalSamples(0.0, 16.0, times.size(), noise);

    maths::CTimeSeriesDecomposition decomposition(0.01);

    //std::ofstream file;
    //file.open("results.m");
    //file << "hold on;\n";
    //file << "t = " << core::CContainerPrinter::print(times) << ";\n";
    //file << "f = " << core::CContainerPrinter::print(timeseries) << ";\n";
    //file << "plot(t, f);";
    //TDoubleVec f;
    //TDoubleVec r;

    double totalSumResidual = 0.0;
    double totalMaxResidual = 0.0;
    double totalSumValue = 0.0;
    double totalMaxValue = 0.0;
    double totalPercentileError = 0.0;
    double meanSlope = 0.0;
    double refinements = 0.0;

    core_t::TTime lastWeek = 0;
    for (std::size_t i = 0u; i < times.size(); ++i)
    {
        core_t::TTime time = times[i];
        double value = timeseries[i] + noise[i];
        decomposition.addPoint(time, value);

        if (time >= lastWeek + week)
        {
            LOG_DEBUG("Processing week");

            double sumResidual = 0.0;
            double maxResidual = 0.0;
            double sumValue = 0.0;
            double maxValue = 0.0;
            double percentileError = 0.0;

            for (core_t::TTime t = lastWeek; t < lastWeek + week; t += halfHour)
            {
                TDoubleDoublePr baseline = decomposition.baseline(t + week, 70.0);

                double residual = ::fabs(timeseries[t / halfHour] - mean(baseline));
                sumResidual += residual;
                maxResidual = std::max(maxResidual, residual);
                sumValue += ::fabs(timeseries[t / halfHour]);
                maxValue = std::max(maxValue, ::fabs(timeseries[t / halfHour]));
                percentileError += std::max(std::max(baseline.first - timeseries[t / halfHour],
                                                     timeseries[t / halfHour] - baseline.second), 0.0);

                //f.push_back(mean(baseline));
                //r.push_back(residual);
            }

            LOG_DEBUG("'sum residual' / 'sum value' = " << sumResidual / sumValue);
            LOG_DEBUG("'max residual' / 'max value' = " << maxResidual / maxValue);
            LOG_DEBUG("70% error = " << percentileError / sumValue);

            if (time >= 2 * week)
            {
                CPPUNIT_ASSERT(sumResidual < 0.21 * sumValue);
                CPPUNIT_ASSERT(maxResidual < 0.25 * maxValue);
                CPPUNIT_ASSERT(percentileError < 0.18 * sumValue);

                totalSumResidual += sumResidual;
                totalMaxResidual += maxResidual;
                totalSumValue += sumValue;
                totalMaxValue += maxValue;
                totalPercentileError += percentileError;

                CPPUNIT_ASSERT_EQUAL(std::size_t(2), decomposition.seasonalComponents().size());
                if (decomposition.seasonalComponents()[1].initialized())
                {
                    double slope = decomposition.seasonalComponents()[1].valueSpline().absSlope();
                    meanSlope += slope;
                    LOG_DEBUG("weekly |slope| = " << slope);

                    CPPUNIT_ASSERT(slope < 0.0018);
                    refinements += 1.0;
                }
            }

            decomposition.propagateForwardsTo(time);
            lastWeek += week;
        }
    }

    LOG_DEBUG("total 'sum residual' / 'sum value' = " << totalSumResidual / totalSumValue);
    LOG_DEBUG("total 'max residual' / 'max value' = " << totalMaxResidual / totalMaxValue);
    LOG_DEBUG("total 70% error = " << totalPercentileError / totalSumValue);

    CPPUNIT_ASSERT(totalSumResidual < 0.09 * totalSumValue);
    CPPUNIT_ASSERT(totalMaxResidual < 0.18 * totalMaxValue);
    CPPUNIT_ASSERT(totalPercentileError < 0.06 * totalSumValue);

    meanSlope /= refinements;
    LOG_DEBUG("mean weekly |slope| = " << meanSlope);
    CPPUNIT_ASSERT(meanSlope < 0.0015);

    //file << "fe = " << core::CContainerPrinter::print(f) << ";\n";
    //file << "r = " << core::CContainerPrinter::print(r) << ";\n";
    //file << "plot(t(1:length(fe)), fe, 'r');\n";
    //file << "scatter(t(1:length(r)), r, 15, 'k', 'x');\n";
}

void CTimeSeriesDecompositionTest::testWeekend(void)
{
    LOG_DEBUG("+---------------------------------------------+");
    LOG_DEBUG("|  CTimeSeriesDecompositionTest::testWeekend  |");
    LOG_DEBUG("+---------------------------------------------+");

    const core_t::TTime halfHour = 1800;
    const core_t::TTime day = 86400;
    const core_t::TTime week = 604800;

    double weights[] = { 0.1, 0.1, 1.0, 1.0, 1.0, 1.0, 1.0 };

    TTimeVec times;
    TDoubleVec timeseries;
    for (core_t::TTime time = 0; time < 100 * week; time += halfHour)
    {
        double weight = weights[(time / day) % 7];
        double daily = 100.0 * ::sin(boost::math::double_constants::two_pi
                                     * static_cast<double>(time)
                                     / static_cast<double>(day));
        times.push_back(time);
        timeseries.push_back(weight * daily);
    }

    test::CRandomNumbers rng;
    TDoubleVec noise;
    rng.generateNormalSamples(0.0, 20.0, times.size(), noise);

    maths::CTimeSeriesDecomposition decomposition(0.01);

    //std::ofstream file;
    //file.open("results.m");
    //file << "hold on;\n";
    //file << "t = " << core::CContainerPrinter::print(times) << ";\n";
    //file << "f = " << core::CContainerPrinter::print(timeseries) << ";\n";
    //file << "plot(t, f);";
    //TDoubleVec f;
    //TDoubleVec r;

    double totalSumResidual = 0.0;
    double totalMaxResidual = 0.0;
    double totalSumValue = 0.0;
    double totalMaxValue = 0.0;
    double totalPercentileError = 0.0;

    core_t::TTime lastWeek = 0;
    for (std::size_t i = 0u; i < times.size(); ++i)
    {
        core_t::TTime time = times[i];
        double value = timeseries[i] + noise[i];
        decomposition.addPoint(time, value);

        if (time >= lastWeek + week)
        {
            LOG_DEBUG("Processing week");

            double sumResidual = 0.0;
            double maxResidual = 0.0;
            double sumValue = 0.0;
            double maxValue = 0.0;
            double percentileError = 0.0;

            for (core_t::TTime t = lastWeek;
                 t < lastWeek + week;
                 t += halfHour)
            {
                TDoubleDoublePr baseline = decomposition.baseline(t + week, 70.0);

                double residual = ::fabs(timeseries[t / halfHour] - mean(baseline));
                sumResidual += residual;
                maxResidual = std::max(maxResidual, residual);
                sumValue += ::fabs(timeseries[t / halfHour]);
                maxValue = std::max(maxValue, ::fabs(timeseries[t / halfHour]));
                percentileError += std::max(std::max(baseline.first - timeseries[t / halfHour],
                                                     timeseries[t / halfHour] - baseline.second), 0.0);

                //f.push_back(mean(baseline));
                //r.push_back(residual);
            }

            LOG_DEBUG("'sum residual' / 'sum value' = " << sumResidual / sumValue);
            LOG_DEBUG("'max residual' / 'max value' = " << maxResidual / maxValue);
            LOG_DEBUG("70% error = " << percentileError / sumValue);

            if (time >= 3 * week)
            {
                CPPUNIT_ASSERT(sumResidual < 0.07 * sumValue);
                CPPUNIT_ASSERT(maxResidual < 0.12 * maxValue);
                CPPUNIT_ASSERT(percentileError < 0.03 * sumValue);

                totalSumResidual += sumResidual;
                totalMaxResidual += maxResidual;
                totalSumValue += sumValue;
                totalMaxValue += maxValue;
                totalPercentileError += percentileError;
            }

            decomposition.propagateForwardsTo(time);
            lastWeek += week;
        }
    }

    //file << "fe = " << core::CContainerPrinter::print(f) << ";\n";
    //file << "r = " << core::CContainerPrinter::print(r) << ";\n";
    //file << "plot(t(1:length(fe)), fe, 'r');\n";
    //file << "scatter(t(1:length(r)), r, 15, 'k', 'x');\n";

    LOG_DEBUG("total 'sum residual' / 'sum value' = " << totalSumResidual / totalSumValue);
    LOG_DEBUG("total 'max residual' / 'max value' = " << totalMaxResidual / totalMaxValue);
    LOG_DEBUG("total 70% error = " << totalPercentileError / totalSumValue);

    CPPUNIT_ASSERT(totalSumResidual < 0.037 * totalSumValue);
    CPPUNIT_ASSERT(totalMaxResidual < 0.076 * totalMaxValue);
    CPPUNIT_ASSERT(totalPercentileError < 0.01 * totalSumValue);
}

void CTimeSeriesDecompositionTest::testSinglePeriodicity(void)
{
    LOG_DEBUG("+-------------------------------------------------------+");
    LOG_DEBUG("|  CTimeSeriesDecompositionTest::testSinglePeriodicity  |");
    LOG_DEBUG("+-------------------------------------------------------+");

    const core_t::TTime halfHour = 1800;
    const core_t::TTime day = 86400;
    const core_t::TTime week = 604800;

    TTimeVec times;
    TDoubleVec timeseries;
    for (core_t::TTime time = 0; time < 10 * week + 1; time += halfHour)
    {
        double daily = 100.0 + 100.0 * ::sin(boost::math::double_constants::two_pi
                                             * static_cast<double>(time)
                                             / static_cast<double>(day));
        times.push_back(time);
        timeseries.push_back(daily);
    }

    const double noiseMean = 20.0;
    const double noiseVariance = 16.0;
    test::CRandomNumbers rng;
    TDoubleVec noise;
    rng.generateNormalSamples(noiseMean, noiseVariance, times.size(), noise);

    maths::CTimeSeriesDecomposition decomposition(0.01);

    //std::ofstream file;
    //file.open("results.m");
    //file << "hold on;\n";
    //file << "t = " << core::CContainerPrinter::print(times) << ";\n";
    //file << "f = " << core::CContainerPrinter::print(timeseries) << ";\n";
    //file << "plot(t, f);\n";
    //TDoubleVec f;
    //TDoubleVec r;

    double totalSumResidual = 0.0;
    double totalMaxResidual = 0.0;
    double totalSumValue = 0.0;
    double totalMaxValue = 0.0;
    double totalPercentileError = 0.0;

    core_t::TTime lastWeek = 0;
    for (std::size_t i = 0u; i < times.size(); ++i)
    {
        core_t::TTime time = times[i];
        double value = timeseries[i] + noise[i];
        decomposition.addPoint(time, value);

        if (time >= lastWeek + week)
        {
            LOG_DEBUG("Processing week");

            double sumResidual = 0.0;
            double maxResidual = 0.0;
            double sumValue = 0.0;
            double maxValue = 0.0;
            double percentileError = 0.0;

            for (core_t::TTime t = lastWeek;
                 t < lastWeek + week;
                 t += halfHour)
            {
                TDoubleDoublePr baseline = decomposition.baseline(t + week, 70.0);

                double residual = ::fabs(timeseries[t / halfHour] + noiseMean - mean(baseline));
                sumResidual += residual;
                maxResidual = std::max(maxResidual, residual);
                sumValue += ::fabs(timeseries[t / halfHour]);
                maxValue = std::max(maxValue, ::fabs(timeseries[t / halfHour]));
                percentileError += std::max(std::max(baseline.first - (timeseries[t / halfHour] + noiseMean),
                                                     (timeseries[t / halfHour] + noiseMean) - baseline.second), 0.0);

                //f.push_back(mean(baseline));
                //r.push_back(residual);
            }

            LOG_DEBUG("'sum residual' / 'sum value' = " << sumResidual / sumValue);
            LOG_DEBUG("'max residual' / 'max value' = " << maxResidual / maxValue);
            LOG_DEBUG("70% error = " << percentileError / sumValue);

            if (time >= 1 * week)
            {
                CPPUNIT_ASSERT(sumResidual < 0.029 * sumValue);
                CPPUNIT_ASSERT(maxResidual < 0.047 * maxValue);
                CPPUNIT_ASSERT(percentileError < 0.01 * sumValue);

                totalSumResidual += sumResidual;
                totalMaxResidual += maxResidual;
                totalSumValue += sumValue;
                totalMaxValue += maxValue;
                totalPercentileError += percentileError;

                // Check that only the daily component has been initialized.
                const TComponentVec &components = decomposition.seasonalComponents();
                CPPUNIT_ASSERT_EQUAL(std::size_t(1), components.size());
                CPPUNIT_ASSERT_EQUAL(day, components[0].time().period());
                CPPUNIT_ASSERT(components[0].initialized());
            }

            decomposition.propagateForwardsTo(time);
            lastWeek += week;
        }
    }

    LOG_DEBUG("total 'sum residual' / 'sum value' = " << totalSumResidual / totalSumValue);
    LOG_DEBUG("total 'max residual' / 'max value' = " << totalMaxResidual / totalMaxValue);
    LOG_DEBUG("total 70% error = " << totalPercentileError / totalSumValue);
    CPPUNIT_ASSERT(totalSumResidual < 0.022 * totalSumValue);
    CPPUNIT_ASSERT(totalMaxResidual < 0.04 * totalMaxValue);
    CPPUNIT_ASSERT(totalPercentileError < 0.01 * totalSumValue);

    // Check that only the daily component has been initialized.
    const TComponentVec &components = decomposition.seasonalComponents();
    CPPUNIT_ASSERT_EQUAL(std::size_t(1), components.size());
    CPPUNIT_ASSERT_EQUAL(day, components[0].time().period());
    CPPUNIT_ASSERT(components[0].initialized());

    //file << "fe = " << core::CContainerPrinter::print(f) << ";\n";
    //file << "r = " << core::CContainerPrinter::print(r) << ";\n";
    //file << "plot(t(1:length(fe)), fe, 'r');\n";
    //file << "scatter(t(1:length(r)), r, 15, 'k', 'x');\n";
}

void CTimeSeriesDecompositionTest::testSeasonalOnset(void)
{
    LOG_DEBUG("+---------------------------------------------------+");
    LOG_DEBUG("|  CTimeSeriesDecompositionTest::testSeasonalOnset  |");
    LOG_DEBUG("+---------------------------------------------------+");

    const core_t::TTime hour = 3600;
    const core_t::TTime day = 86400;
    const core_t::TTime week = 604800;
    const double daily[] =
        {
             0.0,  0.0,  0.0,  0.0,  5.0,  5.0,
             5.0, 40.0, 40.0, 40.0, 30.0, 30.0,
            35.0, 35.0, 40.0, 50.0, 60.0, 80.0,
            80.0, 10.0,  5.0,  0.0,  0.0,  0.0
        };
    const double weekly[] =
        {
             0.1, 0.1, 1.2, 1.0, 1.0, 0.9, 1.5
        };

    TTimeVec times;
    TDoubleVec timeseries;
    for (core_t::TTime time = 0; time < 150 * week + 1; time += hour)
    {
        double baseline = 0.0;
        if (time > 10 * week)
        {
            baseline += daily[(time % day) / hour];
            baseline *= weekly[(time % week) / day];
        }
        times.push_back(time);
        timeseries.push_back(baseline);
    }

    test::CRandomNumbers rng;
    TDoubleVec noise;
    rng.generateNormalSamples(0.0, 4.0, times.size(), noise);

    maths::CTimeSeriesDecomposition decomposition(0.01);

    //std::ofstream file;
    //file.open("results.m");
    //file << "hold on;\n";
    //file << "t = " << core::CContainerPrinter::print(times) << ";\n";
    //file << "f = " << core::CContainerPrinter::print(timeseries) << ";\n";
    //file << "plot(t, f, 'r');\n";
    //TDoubleVec f;
    //TDoubleVec r;

    double totalSumResidual = 0.0;
    double totalMaxResidual = 0.0;
    double totalSumValue = 0.0;
    double totalMaxValue = 0.0;
    double totalPercentileError = 0.0;

    core_t::TTime lastWeek = 0;
    for (std::size_t i = 0u; i < times.size(); ++i)
    {
        core_t::TTime time = times[i];
        double value = timeseries[i] + noise[i];
        decomposition.addPoint(time, value);

        if (time >= lastWeek + week)
        {
            LOG_DEBUG("Processing week");

            double sumResidual = 0.0;
            double maxResidual = 0.0;
            double sumValue = 0.0;
            double maxValue = 0.0;
            double percentileError = 0.0;
            for (core_t::TTime t = lastWeek; t < lastWeek + week; t += hour)
            {
                TDoubleDoublePr baseline = decomposition.baseline(t + week, 70.0);

                double residual = ::fabs(timeseries[t / hour] - mean(baseline));
                sumResidual += residual;
                maxResidual = std::max(maxResidual, residual);
                sumValue += ::fabs(timeseries[t / hour]);
                maxValue = std::max(maxValue, ::fabs(timeseries[t / hour]));
                percentileError += std::max(std::max(baseline.first - timeseries[t / hour],
                                                     timeseries[t / hour] - baseline.second), 0.0);

                //f.push_back(mean(baseline));
                //r.push_back(residual);
            }

            LOG_DEBUG("'sum residual' / 'sum value' = "
                      << (sumResidual == 0.0 ? 0.0 : sumResidual / sumValue));
            LOG_DEBUG("'max residual' / 'max value' = "
                      << (maxResidual == 0.0 ? 0.0 : maxResidual / maxValue));
            LOG_DEBUG("70% error = " << (percentileError == 0.0 ? 0.0 : percentileError / sumValue));

            totalSumResidual += sumResidual;
            totalMaxResidual += maxResidual;
            totalSumValue += sumValue;
            totalMaxValue += maxValue;
            totalPercentileError += percentileError;

            const TComponentVec &components = decomposition.seasonalComponents();
            if (time > 14 * week)
            {
                // Check that both components have been initialized.
                CPPUNIT_ASSERT(components.size() > 1);
                CPPUNIT_ASSERT(components[0].initialized());
                CPPUNIT_ASSERT(components[1].initialized());
            }
            else if (time > 12 * week)
            {
                // Check that both components have been initialized.
                CPPUNIT_ASSERT(components.size() == 1);
                CPPUNIT_ASSERT(components[0].initialized());
            }
            else
            {
                // Check that neither component has been initialized.
                CPPUNIT_ASSERT(components.empty());
            }
            decomposition.propagateForwardsTo(time);
            lastWeek += week;
        }
    }

    LOG_DEBUG("total 'sum residual' / 'sum value' = " << totalSumResidual / totalSumValue);
    LOG_DEBUG("total 'max residual' / 'max value' = " << totalMaxResidual / totalMaxValue);
    LOG_DEBUG("total 70% error = " << totalPercentileError / totalSumValue);
    CPPUNIT_ASSERT(totalSumResidual < 0.1 * totalSumValue);
    CPPUNIT_ASSERT(totalMaxResidual < 0.13 * totalMaxValue);
    CPPUNIT_ASSERT(totalPercentileError < 0.05 * totalSumValue);

    //file << "fe = " << core::CContainerPrinter::print(f) << ";\n";
    //file << "r = " << core::CContainerPrinter::print(r) << ";\n";
    //file << "plot(t(1:length(fe)), fe);\n";
    //file << "scatter(t(1:length(r)), r, 15, 'k', 'x');\n";
}

void CTimeSeriesDecompositionTest::testVarianceScale(void)
{
    LOG_DEBUG("+---------------------------------------------------+");
    LOG_DEBUG("|  CTimeSeriesDecompositionTest::testVarianceScale  |");
    LOG_DEBUG("+---------------------------------------------------+");

    // Test that variance scales are correctly computed.

    typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;

    const core_t::TTime day = 86400;
    const core_t::TTime tenMins = 600;

    test::CRandomNumbers rng;

    {
        core_t::TTime time = 0;
        maths::CTimeSeriesDecomposition decomposition(0.01, 600);

        for (std::size_t i = 0u; i < 50; ++i)
        {
            for (core_t::TTime t = 0; t < day; t += tenMins)
            {
                double baseline = 1.0;
                double variance = 1.0;
                if (t >= 3600 && t < 7200)
                {
                    baseline = 5.0;
                    variance = 10.0;
                }
                TDoubleVec value;
                rng.generateNormalSamples(baseline, variance, 1, value);
                decomposition.addPoint(time + t, value[0]);
            }
            time += day;
        }

        double meanVariance = (1.0 * 23.0 + 10.0 * 1.0) / 24.0;
        time -= day;
        TMeanAccumulator error;
        TMeanAccumulator percentileError;
        TMeanAccumulator meanScale;
        for (core_t::TTime t = 0; t < day; t += tenMins)
        {
            double variance = 1.0;
            if (t >= 3600 && t < 7200)
            {
                variance = 10.0;
            }
            double expectedScale = variance / meanVariance;
            TDoubleDoublePr interval = decomposition.scale(time + t, meanVariance, 70.0);
            LOG_DEBUG("time = " << t
                      << ", expectedScale = " << expectedScale
                      << ", scale = " << core::CContainerPrinter::print(interval));
            double scale = (interval.first + interval.second) / 2.0;
            error.add(::fabs(scale - expectedScale));
            meanScale.add(scale);
            percentileError.add(std::max(std::max(interval.first - expectedScale,
                                         expectedScale - interval.second), 0.0));
        }
        LOG_DEBUG("mean error = " << maths::CBasicStatistics::mean(error));
        LOG_DEBUG("mean 70% error = " << maths::CBasicStatistics::mean(percentileError))
        LOG_DEBUG("mean scale = " << maths::CBasicStatistics::mean(meanScale));
        CPPUNIT_ASSERT(maths::CBasicStatistics::mean(error) < 0.2);
        CPPUNIT_ASSERT(maths::CBasicStatistics::mean(percentileError) < 0.01);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, maths::CBasicStatistics::mean(meanScale), 0.04);
    }
    {
        core_t::TTime time = 0;
        maths::CTimeSeriesDecomposition decomposition(0.01, 600);

        for (std::size_t i = 0u; i < 50; ++i)
        {
            for (core_t::TTime t = 0; t < day; t += tenMins)
            {
                double baseline = 5.0 * ::sin(boost::math::double_constants::two_pi
                                              * static_cast<double>(t)
                                              / static_cast<double>(day));
                double variance = 1.0;
                if (t >= 3600 && t < 7200)
                {
                    variance = 10.0;
                }
                TDoubleVec value;
                rng.generateNormalSamples(0.0, variance, 1, value);
                decomposition.addPoint(time + t, baseline + value[0]);
            }
            time += day;
        }

        double meanVariance = (1.0 * 23.0 + 10.0 * 1.0) / 24.0;
        time -= day;
        TMeanAccumulator error;
        TMeanAccumulator percentileError;
        TMeanAccumulator meanScale;
        for (core_t::TTime t = 0; t < day; t += tenMins)
        {
            double variance = 1.0;
            if (t >= 3600 && t < 7200)
            {
                variance = 10.0;
            }
            double expectedScale = variance / meanVariance;
            TDoubleDoublePr interval = decomposition.scale(time + t, meanVariance, 70.0);
            LOG_DEBUG("time = " << t
                      << ", expectedScale = " << expectedScale
                      << ", scale = " << core::CContainerPrinter::print(interval));
            double scale = (interval.first + interval.second) / 2.0;
            error.add(::fabs(scale - expectedScale));
            meanScale.add(scale);
            percentileError.add(std::max(std::max(interval.first - expectedScale,
                                         expectedScale - interval.second), 0.0));
        }
        LOG_DEBUG("mean error = " << maths::CBasicStatistics::mean(error));
        LOG_DEBUG("mean 70% error = " << maths::CBasicStatistics::mean(percentileError));
        LOG_DEBUG("mean scale = " << maths::CBasicStatistics::mean(meanScale));
        CPPUNIT_ASSERT(maths::CBasicStatistics::mean(error) < 0.26);
        CPPUNIT_ASSERT(maths::CBasicStatistics::mean(percentileError) < 0.05);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, maths::CBasicStatistics::mean(meanScale), 0.01);
    }
}

void CTimeSeriesDecompositionTest::testEmcProblemCase(void)
{
    LOG_DEBUG("+----------------------------------------------------+");
    LOG_DEBUG("|  CTimeSeriesDecompositionTest::testEmcProblemCase  |");
    LOG_DEBUG("+----------------------------------------------------+");

    const core_t::TTime day = 86400;
    const core_t::TTime week = 604800;

    TTimeDoublePrVec timeseries;
    core_t::TTime startTime;
    core_t::TTime endTime;
    CPPUNIT_ASSERT(maths::CTimeSeriesTestData::parse("testfiles/emc.csv",
                                                     timeseries,
                                                     startTime,
                                                     endTime,
                                                     "^([0-9]+),([0-9\\.]+)"));
    CPPUNIT_ASSERT(!timeseries.empty());

    LOG_DEBUG("timeseries = " << core::CContainerPrinter::print(timeseries.begin(),
                                                                timeseries.begin() + 10)
              << " ...");

    double totalSumResidual = 0.0;
    double totalMaxResidual = 0.0;
    double totalSumValue = 0.0;
    double totalMaxValue = 0.0;
    double totalPercentileError = 0.0;

    maths::CTimeSeriesDecomposition decomposition(0.01, 300);
    maths::CNormalMeanPrecConjugate model =
            maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData, 0.01);

    core_t::TTime lastWeek = (startTime / week + 1) * week;
    TTimeDoublePrVec lastWeekTimeseries;
    for (std::size_t i = 0u; i < timeseries.size(); ++i)
    {
        core_t::TTime time = timeseries[i].first;
        double value = timeseries[i].second;

        if (time > lastWeek + week)
        {
            LOG_DEBUG("Processing week");

            double sumResidual = 0.0;
            double maxResidual = 0.0;
            double sumValue = 0.0;
            double maxValue = 0.0;
            double percentileError = 0.0;

            for (std::size_t j = 0u; j < lastWeekTimeseries.size(); ++j)
            {
                TDoubleDoublePr baseline = decomposition.baseline(lastWeekTimeseries[j].first, 70.0);

                double residual = ::fabs(lastWeekTimeseries[j].second - mean(baseline));
                sumResidual += residual;
                maxResidual = std::max(maxResidual, residual);
                sumValue += ::fabs(lastWeekTimeseries[j].second);
                maxValue = std::max(maxValue, ::fabs(lastWeekTimeseries[j].second));
                percentileError += std::max(std::max(baseline.first - lastWeekTimeseries[j].second,
                                                     lastWeekTimeseries[j].second - baseline.second), 0.0);
            }

            LOG_DEBUG("'sum residual' / 'sum value' = "
                      << (sumResidual == 0.0 ? 0.0 : sumResidual / sumValue));
            LOG_DEBUG("'max residual' / 'max value' = "
                      << (maxResidual == 0.0 ? 0.0 : maxResidual / maxValue));
            LOG_DEBUG("70% error = " << percentileError / sumValue);

            if (time >= startTime + week)
            {
                totalSumResidual += sumResidual;
                totalMaxResidual += maxResidual;
                totalSumValue += sumValue;
                totalMaxValue += maxValue;
                totalPercentileError += percentileError;
            }

            lastWeekTimeseries.clear();
            lastWeek += week;
        }
        if (time > lastWeek)
        {
            lastWeekTimeseries.push_back(timeseries[i]);
        }

        if (decomposition.addPoint(time, value))
        {
            model.setToNonInformative(0.0, 0.01);
        }
        decomposition.propagateForwardsTo(time);
        model.addSamples(maths_t::TWeightStyleVec(1, maths_t::E_SampleCountWeight),
                         TDoubleVec(1, decomposition.detrend(time, value, 70.0)),
                         TDoubleVecVec(1, TDoubleVec(1, 1.0)));
    }

    LOG_DEBUG("total 'sum residual' / 'sum value' = " << totalSumResidual / totalSumValue);
    LOG_DEBUG("total 'max residual' / 'max value' = " << totalMaxResidual / totalMaxValue);
    LOG_DEBUG("total 70% error = " << totalPercentileError / totalSumValue);

    CPPUNIT_ASSERT(totalSumResidual < 0.21 * totalSumValue);
    CPPUNIT_ASSERT(totalMaxResidual < 0.37 * totalMaxValue);
    CPPUNIT_ASSERT(totalPercentileError < 0.1 * totalSumValue);

    //std::ofstream file;
    //file.open("results.m");
    //TTimeVec times;
    //TDoubleVec raw;
    //TDoubleVec baseline;
    //TDoubleVec scales;
    //TDoubleVec probs;

    double pMinScaled = 1.0;
    double pMinUnscaled = 1.0;
    for (std::size_t i = 0u; timeseries[i].first < startTime + day; ++i)
    {
        core_t::TTime time = timeseries[i].first;
        double value = timeseries[i].second;
        double variance = model.marginalLikelihoodVariance();

        double lb, ub;
        maths_t::ETail tail;
        model.probabilityOfLessLikelySamples(
                      maths_t::E_TwoSided,
                      maths_t::TWeightStyleVec(1, maths_t::E_SampleSeasonalVarianceScaleWeight),
                      TDoubleVec(1, decomposition.detrend(time, value, 70.0)),
                      TDoubleVecVec(1, TDoubleVec(1, std::max(decomposition.scale(time, variance, 70.0).second, 0.25))),
                      lb, ub, tail);
        double pScaled = (lb + ub) / 2.0;
        pMinScaled = std::min(pMinScaled, pScaled);

        //times.push_back(time);
        //raw.push_back(value);
        //baseline.push_back(mean(decomposition.baseline(time, 70.0)));
        //scales.push_back(mean(decomposition.varianceScale(time, 70.0)));
        //probs.push_back(-::log(pScaled));

        model.probabilityOfLessLikelySamples(
                      maths_t::E_TwoSided,
                      maths_t::TWeightStyleVec(1, maths_t::E_SampleSeasonalVarianceScaleWeight),
                      TDoubleVec(1, decomposition.detrend(time, value, 70.0)),
                      TDoubleVecVec(1, TDoubleVec(1, 1.0)),
                      lb, ub, tail);
        double pUnscaled = (lb + ub) / 2.0;
        pMinUnscaled = std::min(pMinUnscaled, pUnscaled);
    }

    //file << "hold on;\n";
    //file << "t = " << core::CContainerPrinter::print(times) << ";\n";
    //file << "r = " << core::CContainerPrinter::print(raw) << ";\n";
    //file << "b = " << core::CContainerPrinter::print(baseline) << ";\n";
    //file << "s = " << core::CContainerPrinter::print(scales) << ";\n";
    //file << "p = " << core::CContainerPrinter::print(probs) << ";\n";
    //file << "subplot(3,1,1); hold on; plot(t, r, 'b'); plot(t, b, 'r');\n";
    //file << "subplot(3,1,2); plot(t, s, 'b');\n";
    //file << "subplot(3,1,3); plot(t, p, 'b');\n";

    LOG_DEBUG("pMinScaled = " << pMinScaled);
    LOG_DEBUG("pMinUnscaled = " << pMinUnscaled);
    CPPUNIT_ASSERT(pMinScaled > 1e11 * pMinUnscaled);
}

void CTimeSeriesDecompositionTest::testBlueCoatProblemCase(void)
{
    LOG_DEBUG("+---------------------------------------------------------+");
    LOG_DEBUG("|  CTimeSeriesDecompositionTest::testBlueCoatProblemCase  |");
    LOG_DEBUG("+---------------------------------------------------------+");

    typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;

    const core_t::TTime tenMinutes = 600;
    const core_t::TTime day = 86400;
    const core_t::TTime week = 604800;

    TTimeDoublePrVec timeseries;
    core_t::TTime startTime;
    core_t::TTime endTime;
    CPPUNIT_ASSERT(maths::CTimeSeriesTestData::parse("testfiles/bluecoat.csv",
                                                     timeseries,
                                                     startTime,
                                                     endTime,
                                                     "^([0-9]+),([0-9\\.]+)"));
    CPPUNIT_ASSERT(!timeseries.empty());

    LOG_DEBUG("timeseries = " << core::CContainerPrinter::print(timeseries.begin(),
                                                                timeseries.begin() + 10)
              << " ...");

    //std::ofstream file;
    //file.open("results.m");
    //TDoubleVec times;
    //TDoubleVec values;
    //TDoubleVec f;
    //TDoubleVec r;

    double totalSumResidual = 0.0;
    double totalMaxResidual = 0.0;
    double totalSumValue = 0.0;
    double totalMaxValue = 0.0;
    double totalPercentileError = 0.0;

    maths::CTimeSeriesDecomposition decomposition(0.01, 300);

    core_t::TTime lastWeek = (startTime / week + 1) * week;
    TTimeDoublePrVec lastWeekTimeseries;
    for (std::size_t i = 0u; i < timeseries.size(); ++i)
    {
        core_t::TTime time = timeseries[i].first;
        double value = timeseries[i].second;

        if (time > lastWeek + week)
        {
            LOG_DEBUG("Processing week");

            double sumResidual = 0.0;
            double maxResidual = 0.0;
            double sumValue = 0.0;
            double maxValue = 0.0;
            double percentileError = 0.0;

            for (std::size_t j = 0u; j < lastWeekTimeseries.size(); ++j)
            {
                TDoubleDoublePr baseline = decomposition.baseline(lastWeekTimeseries[j].first, 70.0);

                double residual = ::fabs(lastWeekTimeseries[j].second - mean(baseline));
                sumResidual += residual;
                maxResidual = std::max(maxResidual, residual);
                sumValue += ::fabs(lastWeekTimeseries[j].second);
                maxValue = std::max(maxValue, ::fabs(lastWeekTimeseries[j].second));
                percentileError += std::max(std::max(baseline.first - lastWeekTimeseries[j].second,
                                                     lastWeekTimeseries[j].second - baseline.second), 0.0);

                //times.push_back(lastWeekTimeseries[j].first);
                //values.push_back(lastWeekTimeseries[j].second);
                //f.push_back(mean(baseline));
                //r.push_back(residual);
            }

            LOG_DEBUG("'sum residual' / 'sum value' = " << sumResidual / sumValue);
            LOG_DEBUG("'max residual' / 'max value' = " << maxResidual / maxValue);
            LOG_DEBUG("70% error = " << percentileError / sumValue);

            if (time >= startTime + 3 * week)
            {
                totalSumResidual += sumResidual;
                totalMaxResidual += maxResidual;
                totalSumValue += sumValue;
                totalMaxValue += maxValue;
                totalPercentileError += percentileError;
            }

            lastWeekTimeseries.clear();
            lastWeek += week;
        }
        if (time > lastWeek)
        {
            lastWeekTimeseries.push_back(timeseries[i]);
        }

        decomposition.addPoint(time, value);
        decomposition.propagateForwardsTo(time);
    }

    LOG_DEBUG("total 'sum residual' / 'sum value' = "
              << totalSumResidual / totalSumValue);
    LOG_DEBUG("total 'max residual' / 'max value' = "
              << totalMaxResidual / totalMaxValue);
    LOG_DEBUG("total 70% error = " << totalPercentileError / totalSumValue);

    CPPUNIT_ASSERT(totalSumResidual < 0.22 * totalSumValue);
    CPPUNIT_ASSERT(totalMaxResidual < 0.71 * totalMaxValue);
    CPPUNIT_ASSERT(totalPercentileError < 0.14 * totalSumValue);

    //file << "hold on;\n";
    //file << "t = " << core::CContainerPrinter::print(times) << ";\n";
    //file << "f = " << core::CContainerPrinter::print(values) << ";\n";
    //file << "plot(t, f, 'r');\n";
    //file << "fe = " << core::CContainerPrinter::print(f) << ";\n";
    //file << "r = " << core::CContainerPrinter::print(r) << ";\n";
    //file << "plot(t(1:length(fe)), fe);\n";
    //file << "scatter(t(1:length(r)), r, 15, 'k', 'x');\n";

    TMeanAccumulator scale;
    double variance = decomposition.meanVariance();
    core_t::TTime time = maths::CIntegerTools::floor(endTime, day);
    for (core_t::TTime t = time; t < time + week; t += tenMinutes)
    {
        scale.add(mean(decomposition.scale(t, variance, 70.0)));
    }

    LOG_DEBUG("scale = " << maths::CBasicStatistics::mean(scale));
    CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, maths::CBasicStatistics::mean(scale), 0.03);
}

void CTimeSeriesDecompositionTest::testUMichProblemCase(void)
{
    LOG_DEBUG("+------------------------------------------------------+");
    LOG_DEBUG("|  CTimeSeriesDecompositionTest::testUMichProblemCase  |");
    LOG_DEBUG("+------------------------------------------------------+");

    const core_t::TTime week = 604800;

    TTimeDoublePrVec timeseries;
    core_t::TTime startTime;
    core_t::TTime endTime;
    CPPUNIT_ASSERT(maths::CTimeSeriesTestData::parse("testfiles/umich.csv",
                                                     timeseries,
                                                     startTime,
                                                     endTime,
                                                     maths::CTimeSeriesTestData::CSV_SPLUNK_REGEX,
                                                     maths::CTimeSeriesTestData::CSV_SPLUNK_DATE_FORMAT));
    CPPUNIT_ASSERT(!timeseries.empty());

    LOG_DEBUG("timeseries = " << core::CContainerPrinter::print(timeseries.begin(),
                                                                timeseries.begin() + 10)
              << " ...");

    //std::ofstream file;
    //file.open("results.m");
    //TDoubleVec times;
    //TDoubleVec values;
    //TDoubleVec f;
    //TDoubleVec r;

    double totalSumResidual = 0.0;
    double totalMaxResidual = 0.0;
    double totalSumValue = 0.0;
    double totalMaxValue = 0.0;
    double totalPercentileError = 0.0;

    maths::CTimeSeriesDecomposition decomposition(0.01, 1800);

    core_t::TTime lastWeek = (startTime / week + 1) * week;
    TTimeDoublePrVec lastWeekTimeseries;
    for (std::size_t i = 0u; i < timeseries.size(); ++i)
    {
        core_t::TTime time = timeseries[i].first;
        double value = timeseries[i].second;

        if (time > lastWeek + week)
        {
            LOG_DEBUG("Processing week");

            double sumResidual = 0.0;
            double maxResidual = 0.0;
            double sumValue = 0.0;
            double maxValue = 0.0;
            double percentileError = 0.0;

            for (std::size_t j = 0u; j < lastWeekTimeseries.size(); ++j)
            {
                TDoubleDoublePr baseline = decomposition.baseline(lastWeekTimeseries[j].first, 70.0);

                double residual = ::fabs(lastWeekTimeseries[j].second - mean(baseline));
                sumResidual += residual;
                maxResidual = std::max(maxResidual, residual);
                sumValue += ::fabs(lastWeekTimeseries[j].second);
                maxValue = std::max(maxValue, ::fabs(lastWeekTimeseries[j].second));
                percentileError += std::max(std::max(baseline.first - lastWeekTimeseries[j].second,
                                                     lastWeekTimeseries[j].second - baseline.second), 0.0);

                //times.push_back(lastWeekTimeseries[j].first);
                //values.push_back(lastWeekTimeseries[j].second);
                //f.push_back(mean(baseline));
                //r.push_back(residual);
            }

            LOG_DEBUG("'sum residual' / 'sum value' = "
                      << (sumResidual == 0.0 ? 0.0 : sumResidual / sumValue));
            LOG_DEBUG("'max residual' / 'max value' = "
                      << (maxResidual == 0.0 ? 0.0 : maxResidual / maxValue));
            LOG_DEBUG("70% error = " << percentileError / sumValue);

            if (time >= startTime + 3 * week)
            {
                totalSumResidual += sumResidual;
                totalMaxResidual += maxResidual;
                totalSumValue += sumValue;
                totalMaxValue += maxValue;
                totalPercentileError += percentileError;
            }

            lastWeekTimeseries.clear();
            lastWeek += week;
        }
        if (time > lastWeek)
        {
            lastWeekTimeseries.push_back(timeseries[i]);
        }

        decomposition.addPoint(time, value);
        decomposition.propagateForwardsTo(time);
    }

    LOG_DEBUG("total 'sum residual' / 'sum value' = " << totalSumResidual / totalSumValue);
    LOG_DEBUG("total 'max residual' / 'max value' = " << totalMaxResidual / totalMaxValue);
    LOG_DEBUG("total 70% error = " << totalPercentileError / totalSumValue);

    CPPUNIT_ASSERT(totalSumResidual < 0.17 * totalSumValue);
    CPPUNIT_ASSERT(totalMaxResidual < 0.51 * totalMaxValue);
    CPPUNIT_ASSERT(totalPercentileError < 0.08 * totalSumValue);

    //file << "hold on;\n";
    //file << "t = " << core::CContainerPrinter::print(times) << ";\n";
    //file << "f = " << core::CContainerPrinter::print(values) << ";\n";
    //file << "plot(t, f, 'r');\n";
    //file << "fe = " << core::CContainerPrinter::print(f) << ";\n";
    //file << "r = " << core::CContainerPrinter::print(r) << ";\n";
    //file << "plot(t(1:length(fe)), fe);\n";
    //file << "scatter(t(1:length(r)), r, 15, 'k', 'x');\n";
}

void CTimeSeriesDecompositionTest::testLongTermTrend(void)
{
    LOG_DEBUG("+---------------------------------------------------+");
    LOG_DEBUG("|  CTimeSeriesDecompositionTest::testLongTermTrend  |");
    LOG_DEBUG("+---------------------------------------------------+");

    const core_t::TTime halfHour = 1800;
    const core_t::TTime day = 86400;
    const core_t::TTime length = 120 * day;

    TTimeVec times;
    TDoubleVec timeseries;

    test::CRandomNumbers rng;
    TDoubleVec noise;
    rng.generateNormalSamples(0.0, 25.0, length / halfHour, noise);

    //std::ofstream file;
    //file.open("results.m");
    //TDoubleVec f;
    //TDoubleVec values;

    LOG_DEBUG("Linear Ramp")
    {
        for (core_t::TTime time = 0; time < length; time += halfHour)
        {
            times.push_back(time);
            timeseries.push_back(static_cast<double>(time) / static_cast<double>(day));
        }

        maths::CTimeSeriesDecomposition decomposition(0.024, halfHour);

        double totalSumResidual = 0.0;
        double totalMaxResidual = 0.0;
        double totalSumValue = 0.0;
        double totalMaxValue = 0.0;
        core_t::TTime lastDay = times[0];

        for (std::size_t i = 0u; i < times.size(); ++i)
        {
            decomposition.addPoint(times[i], timeseries[i] + noise[i]);
            decomposition.propagateForwardsTo(times[i]);

            if (times[i] > lastDay + day)
            {
                LOG_DEBUG("Processing day " << times[i]);

                if (decomposition.initialized())
                {
                    double sumResidual = 0.0;
                    double maxResidual = 0.0;
                    double sumValue = 0.0;
                    double maxValue = 0.0;

                    TDoubleVec baselines;

                    for (std::size_t j = i - 48; j < i; ++j)
                    {
                        TDoubleDoublePr baseline = decomposition.baseline(times[j], 70.0);
                        baselines.push_back(mean(baseline));
                        double residual = ::fabs(timeseries[j] - mean(baseline));
                        sumResidual += residual;
                        maxResidual = std::max(maxResidual, residual);
                        sumValue += ::fabs(timeseries[j]);
                        maxValue = std::max(maxValue, ::fabs(timeseries[j]));
                    }

                    LOG_DEBUG("'sum residual' / 'sum value' = "
                              << (sumResidual == 0.0 ? 0.0 : sumResidual / sumValue));
                    LOG_DEBUG("'max residual' / 'max value' = "
                              << (maxResidual == 0.0 ? 0.0 : maxResidual / maxValue));

                    totalSumResidual += sumResidual;
                    totalMaxResidual += maxResidual;
                    totalSumValue += sumValue;
                    totalMaxValue += maxValue;

                    CPPUNIT_ASSERT(sumResidual / sumValue < 0.03);
                    CPPUNIT_ASSERT(maxResidual / maxValue < 0.04);
                }
                lastDay += day;
            }
            //values.push_back(timeseries[i] + noise[i]);
            //f.push_back(maths::CBasicStatistics::mean(decomposition.baseline(times[i], 0.0)));
        }

        LOG_DEBUG("total 'sum residual' / 'sum value' = " << totalSumResidual / totalSumValue);
        LOG_DEBUG("total 'max residual' / 'max value' = " << totalMaxResidual / totalMaxValue);

        CPPUNIT_ASSERT(totalSumResidual / totalSumValue < 0.004);
        CPPUNIT_ASSERT(totalMaxResidual / totalMaxValue < 0.004);

        //file << "t = " << core::CContainerPrinter::print(times) << ";\n";
        //file << "f = " << core::CContainerPrinter::print(values) << ";\n";
        //file << "fe = " << core::CContainerPrinter::print(f) << ";\n";
        //file << "plot(t, f, 'r');\n";
        //file << "plot(t, fe);\n";
    }

    LOG_DEBUG("Saw Tooth Not Periodic");
    {
        core_t::TTime drops[] =
            {
                0, 30 *day, 50 * day, 60 * day, 85 * day, 100 * day, 115 * day, 120 * day
            };

        times.clear();
        timeseries.clear();

        {
            std::size_t i = 0u;
            for (core_t::TTime time = 0;
                 time < length;
                 time += halfHour, (time > drops[i] ? ++i : i))
            {
                times.push_back(time);
                timeseries.push_back(25.0 * static_cast<double>(time - drops[i-1])
                                          / static_cast<double>(drops[i] - drops[i-1] + 1));
            }
        }

        maths::CTimeSeriesDecomposition decomposition(0.01, halfHour);

        double totalSumResidual = 0.0;
        double totalMaxResidual = 0.0;
        double totalSumValue = 0.0;
        double totalMaxValue = 0.0;
        core_t::TTime lastDay = times[0];

        for (std::size_t i = 0u; i < times.size(); ++i)
        {
            decomposition.addPoint(times[i], timeseries[i] + 0.3*noise[i]);
            decomposition.propagateForwardsTo(times[i]);

            if (times[i] > lastDay + day)
            {
                LOG_DEBUG("Processing day " << times[i]);

                if (decomposition.initialized())
                {
                    double sumResidual = 0.0;
                    double maxResidual = 0.0;
                    double sumValue = 0.0;
                    double maxValue = 0.0;

                    TDoubleVec baselines;

                    for (std::size_t j = i - 48; j < i; ++j)
                    {
                        TDoubleDoublePr baseline = decomposition.baseline(times[j], 70.0);
                        baselines.push_back(mean(baseline));
                        double residual = ::fabs(timeseries[j] - mean(baseline));
                        sumResidual += residual;
                        maxResidual = std::max(maxResidual, residual);
                        sumValue += ::fabs(timeseries[j]);
                        maxValue = std::max(maxValue, ::fabs(timeseries[j]));
                    }

                    LOG_DEBUG("'sum residual' / 'sum value' = "
                              << (sumResidual == 0.0 ? 0.0 : sumResidual / sumValue));
                    LOG_DEBUG("'max residual' / 'max value' = "
                              << (maxResidual == 0.0 ? 0.0 : maxResidual / maxValue));

                    totalSumResidual += sumResidual;
                    totalMaxResidual += maxResidual;
                    totalSumValue += sumValue;
                    totalMaxValue += maxValue;
                }
                lastDay += day;
            }
            //values.push_back(timeseries[i] + 0.3*noise[i]);
            //f.push_back(maths::CBasicStatistics::mean(decomposition.baseline(times[i], 0.0)));
        }

        LOG_DEBUG("total 'sum residual' / 'sum value' = " << totalSumResidual / totalSumValue);
        LOG_DEBUG("total 'max residual' / 'max value' = " << totalMaxResidual / totalMaxValue);

        CPPUNIT_ASSERT(totalSumResidual / totalSumValue < 0.34);
        CPPUNIT_ASSERT(totalMaxResidual / totalMaxValue < 0.375);

        //file << "t = " << core::CContainerPrinter::print(times) << ";\n";
        //file << "f = " << core::CContainerPrinter::print(values) << ";\n";
        //file << "fe = " << core::CContainerPrinter::print(f) << ";\n";
        //file << "plot(t, f, 'r');\n";
        //file << "plot(t, fe);\n";
    }
}

void CTimeSeriesDecompositionTest::testLongTermTrendAndPeriodicity(void)
{
    LOG_DEBUG("+-----------------------------------------------------------------+");
    LOG_DEBUG("|  CTimeSeriesDecompositionTest::testLongTermTrendAndPeriodicity  |");
    LOG_DEBUG("+-----------------------------------------------------------------+");

    // Test long term mean reverting component plus daily periodic component.

    const core_t::TTime halfHour = 1800;
    const core_t::TTime day = 86400;
    const core_t::TTime length = 120 * day;

    TTimeVec times;
    TDoubleVec timeseries;

    for (core_t::TTime time = 0; time < length; time += halfHour)
    {
        times.push_back(time);
        double x = static_cast<double>(time);
        timeseries.push_back(150.0 + 100.0 * ::sin(  boost::math::double_constants::two_pi * x
                                                   / static_cast<double>(240 * day)
                                                   / (1.0 - x / static_cast<double>(2 * length)))
                                   +  10.0 * ::sin(  boost::math::double_constants::two_pi * x
                                                   / static_cast<double>(day)));
    }

    test::CRandomNumbers rng;

    TDoubleVec noise;
    rng.generateNormalSamples(0.0, 4.0, times.size(), noise);

    std::ofstream file;
    file.open("results.m");
    TDoubleVec f;
    TDoubleVec values;

    maths::CTimeSeriesDecomposition decomposition(0.01, halfHour);

    double totalSumResidual = 0.0;
    double totalMaxResidual = 0.0;
    double totalSumValue = 0.0;
    double totalMaxValue = 0.0;
    core_t::TTime lastDay = times[0];

    for (std::size_t i = 0u; i < times.size(); ++i)
    {
        decomposition.addPoint(times[i], timeseries[i] + 0.3*noise[i]);
        decomposition.propagateForwardsTo(times[i]);

        if (times[i] > lastDay + day)
        {
            LOG_DEBUG("Processing day " << times[i]);

            if (decomposition.initialized())
            {
                double sumResidual = 0.0;
                double maxResidual = 0.0;
                double sumValue = 0.0;
                double maxValue = 0.0;

                TDoubleVec baselines;

                for (std::size_t j = i - 48; j < i; ++j)
                {
                    TDoubleDoublePr baseline = decomposition.baseline(times[j], 70.0);
                    baselines.push_back(mean(baseline));
                    double residual = ::fabs(timeseries[j] - mean(baseline));
                    sumResidual += residual;
                    maxResidual = std::max(maxResidual, residual);
                    sumValue += ::fabs(timeseries[j]);
                    maxValue = std::max(maxValue, ::fabs(timeseries[j]));
                }

                LOG_DEBUG("'sum residual' / 'sum value' = "
                          << (sumResidual == 0.0 ? 0.0 : sumResidual / sumValue));
                LOG_DEBUG("'max residual' / 'max value' = "
                          << (maxResidual == 0.0 ? 0.0 : maxResidual / maxValue));

                totalSumResidual += sumResidual;
                totalMaxResidual += maxResidual;
                totalSumValue += sumValue;
                totalMaxValue += maxValue;

                CPPUNIT_ASSERT(sumResidual / sumValue < 0.45);
                CPPUNIT_ASSERT(maxResidual / maxValue < 0.50);
            }
            lastDay += day;
        }
        values.push_back(timeseries[i] + 0.3*noise[i]);
        f.push_back(maths::CBasicStatistics::mean(decomposition.baseline(times[i], 0.0)));
    }

    LOG_DEBUG("total 'sum residual' / 'sum value' = " << totalSumResidual / totalSumValue);
    LOG_DEBUG("total 'max residual' / 'max value' = " << totalMaxResidual / totalMaxValue);

    CPPUNIT_ASSERT(totalSumResidual / totalSumValue < 0.1);
    CPPUNIT_ASSERT(totalMaxResidual / totalMaxValue < 0.1);

    file << "t = " << core::CContainerPrinter::print(times) << ";\n";
    file << "f = " << core::CContainerPrinter::print(values) << ";\n";
    file << "fe = " << core::CContainerPrinter::print(f) << ";\n";
    file << "plot(t, f, 'r');\n";
    file << "plot(t, fe);\n";
}

void CTimeSeriesDecompositionTest::testSwap(void)
{
    LOG_DEBUG("+------------------------------------------+");
    LOG_DEBUG("|  CTimeSeriesDecompositionTest::testSwap  |");
    LOG_DEBUG("+------------------------------------------+");

    const core_t::TTime halfHour = 1800;
    const core_t::TTime day = 86400;
    const core_t::TTime week = 604800;
    const double decayRate = 0.01;
    const core_t::TTime minimumBucketLength = halfHour;

    TTimeVec times;
    TDoubleVec timeseries1;
    TDoubleVec timeseries2;
    for (core_t::TTime time = 0; time < 10 * week + 1; time += halfHour)
    {
        double daily = 15.0 + 10.0 * ::sin(boost::math::double_constants::two_pi
                                           * static_cast<double>(time)
                                           / static_cast<double>(day));
        times.push_back(time);
        timeseries1.push_back(daily);
        timeseries2.push_back(2.0 * daily);
    }

    test::CRandomNumbers rng;
    TDoubleVec noise;
    rng.generateNormalSamples(20.0, 16.0, 2 * times.size(), noise);

    maths::CTimeSeriesDecomposition decomposition1(decayRate, minimumBucketLength);
    maths::CTimeSeriesDecomposition decomposition2;

    for (std::size_t i = 0u; i < times.size(); i += 2)
    {
        decomposition1.addPoint(times[i], timeseries1[i] + noise[i]);
        decomposition2.addPoint(times[i], timeseries2[i] + noise[i+1]);
    }

    uint64_t checksum1 = decomposition1.checksum();
    uint64_t checksum2 = decomposition2.checksum();

    LOG_DEBUG("checksum1 = " << checksum1 << ", checksum2 = " << checksum2);

    decomposition1.swap(decomposition2);

    CPPUNIT_ASSERT_EQUAL(checksum1, decomposition2.checksum());
    CPPUNIT_ASSERT_EQUAL(checksum2, decomposition1.checksum());
}

void CTimeSeriesDecompositionTest::testPersist(void)
{
    LOG_DEBUG("+---------------------------------------------+");
    LOG_DEBUG("|  CTimeSeriesDecompositionTest::testPersist  |");
    LOG_DEBUG("+---------------------------------------------+");

    // Check that serialization is idempotent.
    const core_t::TTime halfHour = 1800;
    const core_t::TTime day = 86400;
    const core_t::TTime week = 604800;
    const double decayRate = 0.01;
    const core_t::TTime minimumBucketLength = halfHour;

    {
        // First with some known periodic data
        TTimeVec times;
        TDoubleVec timeseries;
        for (core_t::TTime time = 0; time < 10 * week + 1; time += halfHour)
        {
            double daily = 15.0 + 10.0 * ::sin(boost::math::double_constants::two_pi
                                               * static_cast<double>(time)
                                               / static_cast<double>(day));
            times.push_back(time);
            timeseries.push_back(daily);
        }

        test::CRandomNumbers rng;
        TDoubleVec noise;
        rng.generateNormalSamples(20.0, 16.0, times.size(), noise);

        maths::CTimeSeriesDecomposition origDecomposition(decayRate, minimumBucketLength);

        for (std::size_t i = 0u; i < times.size(); ++i)
        {
            origDecomposition.addPoint(times[i], timeseries[i] + noise[i]);
        }

        std::string origXml;
        {
            ml::core::CRapidXmlStatePersistInserter inserter("root");
            origDecomposition.acceptPersistInserter(inserter);
            inserter.toXml(origXml);
        }

        LOG_DEBUG("seasonal component XML representation:\n" << origXml);

        // Restore the XML into a new filter
        core::CRapidXmlParser parser;
        CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
        core::CRapidXmlStateRestoreTraverser traverser(parser);

        maths::CTimeSeriesDecomposition restoredDecomposition(decayRate + 0.1,
                                                              minimumBucketLength,
                                                              maths::CTimeSeriesDecomposition::DEFAULT_COMPONENT_SIZE,
                                                              traverser);

        std::string newXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            restoredDecomposition.acceptPersistInserter(inserter);
            inserter.toXml(newXml);
        }
        CPPUNIT_ASSERT_EQUAL(origXml, newXml);
    }
    {
        // Then with non-periodic data that drops down to the
        // small CRandomizedPeriodic test
        TTimeVec times;
        TDoubleVec timeseries;
        for (core_t::TTime time = 0; time < 10 * week + 1; time += halfHour)
        {
            double daily = 15.0;
            times.push_back(time);
            timeseries.push_back(daily);
        }

        test::CRandomNumbers rng;
        TDoubleVec noise;
        rng.generateNormalSamples(20.0, 16.0, times.size(), noise);

        maths::CTimeSeriesDecomposition origDecomposition(decayRate, minimumBucketLength);

        for (std::size_t i = 0u; i < times.size(); ++i)
        {
            origDecomposition.addPoint(times[i], timeseries[i] + noise[i]);
        }

        std::string origXml;
        {
            ml::core::CRapidXmlStatePersistInserter inserter("root");
            origDecomposition.acceptPersistInserter(inserter);
            inserter.toXml(origXml);
        }

        LOG_DEBUG("seasonal component XML representation:\n" << origXml);

        // Restore the XML into a new filter
        core::CRapidXmlParser parser;
        CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
        core::CRapidXmlStateRestoreTraverser traverser(parser);

        maths::CTimeSeriesDecomposition restoredDecomposition(decayRate + 0.1,
                                                              minimumBucketLength,
                                                              maths::CTimeSeriesDecomposition::DEFAULT_COMPONENT_SIZE,
                                                              traverser);

        std::string newXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            restoredDecomposition.acceptPersistInserter(inserter);
            inserter.toXml(newXml);
        }
        CPPUNIT_ASSERT_EQUAL(origXml, newXml);
    }
}

CppUnit::Test *CTimeSeriesDecompositionTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CTimeSeriesDecompositionTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeSeriesDecompositionTest>(
                                   "CTimeSeriesDecompositionTest::testSuperpositionOfSines",
                                   &CTimeSeriesDecompositionTest::testSuperpositionOfSines) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeSeriesDecompositionTest>(
                                   "CTimeSeriesDecompositionTest::testBlueCoat",
                                   &CTimeSeriesDecompositionTest::testBlueCoat) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeSeriesDecompositionTest>(
                                   "CTimeSeriesDecompositionTest::testMinimizeSlope",
                                   &CTimeSeriesDecompositionTest::testMinimizeSlope) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeSeriesDecompositionTest>(
                                   "CTimeSeriesDecompositionTest::testWeekend",
                                   &CTimeSeriesDecompositionTest::testWeekend) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeSeriesDecompositionTest>(
                                   "CTimeSeriesDecompositionTest::testSinglePeriodicity",
                                   &CTimeSeriesDecompositionTest::testSinglePeriodicity) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeSeriesDecompositionTest>(
                                   "CTimeSeriesDecompositionTest::testSeasonalOnset",
                                   &CTimeSeriesDecompositionTest::testSeasonalOnset) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeSeriesDecompositionTest>(
                                   "CTimeSeriesDecompositionTest::testVarianceScale",
                                   &CTimeSeriesDecompositionTest::testVarianceScale) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeSeriesDecompositionTest>(
                                   "CTimeSeriesDecompositionTest::testEmcProblemCase",
                                   &CTimeSeriesDecompositionTest::testEmcProblemCase) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeSeriesDecompositionTest>(
                                   "CTimeSeriesDecompositionTest::testBlueCoatProblemCase",
                                   &CTimeSeriesDecompositionTest::testBlueCoatProblemCase) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeSeriesDecompositionTest>(
                                   "CTimeSeriesDecompositionTest::testUMichProblemCase",
                                   &CTimeSeriesDecompositionTest::testUMichProblemCase) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeSeriesDecompositionTest>(
                                   "CTimeSeriesDecompositionTest::testLongTermTrend",
                                   &CTimeSeriesDecompositionTest::testLongTermTrend) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeSeriesDecompositionTest>(
                                   "CTimeSeriesDecompositionTest::testLongTermTrendAndPeriodicity",
                                   &CTimeSeriesDecompositionTest::testLongTermTrendAndPeriodicity) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeSeriesDecompositionTest>(
                                   "CTimeSeriesDecompositionTest::testSwap",
                                   &CTimeSeriesDecompositionTest::testSwap) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeSeriesDecompositionTest>(
                                   "CTimeSeriesDecompositionTest::testPersist",
                                   &CTimeSeriesDecompositionTest::testPersist) );
    return suiteOfTests;
}
