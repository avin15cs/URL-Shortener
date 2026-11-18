package com.urlshortener.application.query;

import com.urlshortener.application.mapper.UrlResponseMapper;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UrlQueryService {

    private final UrlRepository urlRepository;
    private final UrlResponseMapper responseMapper;

    public List<UrlResponse> handle(ListUrlsQuery query) {
        return urlRepository.findAllActiveAndNonExpired()
                .stream()
                .map(responseMapper::toResponse)
                .toList();
    }

    public UrlResponse handle(GetUrlByIdQuery query) {
        return responseMapper.toResponse(findById(query.id()));
    }

    public UrlResponse handle(GetUrlStatsQuery query) {
        return responseMapper.toResponse(findById(query.id()));
    }

    public ResolvedUrl handle(ResolveShortCodeQuery query) {
        Url url = urlRepository.findByShortCode(query.shortCode())
                .or(() -> urlRepository.findByCustomAlias(query.shortCode()))
                .orElseThrow(() -> new UrlNotFoundException(
                        "Short URL '" + query.shortCode() + "' not found"));

        if (!url.getIsActive()) {
            throw new UrlNotFoundException(
                    "Short URL '" + query.shortCode() + "' is no longer active");
        }
        if (url.isExpired()) {
            throw new UrlNotFoundException(
                    "Short URL '" + query.shortCode() + "' has expired");
        }

        return new ResolvedUrl(url.getId(), url.getOriginalUrl());
    }

    private Url findById(Long id) {
        return urlRepository.findById(id)
                .orElseThrow(() -> new UrlNotFoundException("URL with ID " + id + " not found"));
    }
}
