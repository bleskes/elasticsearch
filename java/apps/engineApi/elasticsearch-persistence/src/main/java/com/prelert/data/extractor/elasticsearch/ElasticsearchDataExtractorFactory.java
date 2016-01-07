package com.prelert.data.extractor.elasticsearch;

import java.util.Objects;

public class ElasticsearchDataExtractorFactory
{
    private final String m_BaseUrl;
    private final String m_Search;

    public ElasticsearchDataExtractorFactory(String baseUrl, String search)
    {
        m_BaseUrl = Objects.requireNonNull(baseUrl);
        m_Search = Objects.requireNonNull(search);
    }

    public ElasticsearchDataExtractor newExtractor()
    {
//        return new ElasticsearchDataExtractor(m_BaseUrl, m_Search);
        return null;
    }
}
