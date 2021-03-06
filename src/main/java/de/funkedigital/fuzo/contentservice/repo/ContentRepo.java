package de.funkedigital.fuzo.contentservice.repo;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.funkedigital.fuzo.contentservice.models.Content;
import de.funkedigital.fuzo.contentservice.models.ContentSearchRequest;
import de.funkedigital.fuzo.contentservice.models.Section;
import de.funkedigital.fuzo.contentservice.models.StateFields;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Repository
public class ContentRepo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentRepo.class);
    private static final String CONTENT_INDEX = "contents";
    private static final String ID_FIELD = "id";

    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper objectMapper;
    private final String[] includes = new String[]{};
    private final String[] excludes = new String[]{"fields.body"};

    ContentRepo(RestHighLevelClient restHighLevelClient,
                ObjectMapper objectMapper) {
        this.restHighLevelClient = restHighLevelClient;
        this.objectMapper = objectMapper;
    }

    public Content save(Content content) throws IOException {
        IndexRequest indexRequest = new IndexRequest(CONTENT_INDEX, ID_FIELD, String.valueOf(content.getId()));
        indexRequest.source(content.getBody(), XContentType.JSON);
        IndexResponse status = restHighLevelClient.index(indexRequest);
        LOGGER.info("Save status: {}", status);
        return new Content(Long.parseLong(status.getId()));
    }

    public Mono<Content> findById(Long id) {
        LOGGER.info("Fetching content id: {}", id);
        return Mono.create(sink -> restHighLevelClient
                .getAsync(new GetRequest(CONTENT_INDEX, ID_FIELD, String.valueOf(id)),
                        new ActionListener<GetResponse>() {
                            @Override
                            public void onResponse(GetResponse getFields) {
                                if (getFields.isExists()) {
                                    sink.success(new Content(Long.parseLong(getFields.getId()), getFields.getSourceAsString()));
                                } else {
                                    sink.success();
                                }
                            }

                            @Override
                            public void onFailure(Exception e) {
                                sink.error(e);
                            }
                        }));
    }

    public Flux<String> search(ContentSearchRequest contentSearchRequest, StateFields extra) {
        return Flux.fromStream(() -> streamSearch(contentSearchRequest, extra));
    }

    private Stream<String> streamSearch(ContentSearchRequest contentSearchRequest, StateFields extra) {
        try {
            return StreamSupport.stream(restHighLevelClient.search(new SearchRequest(CONTENT_INDEX)
                    .source(new SearchSourceBuilder()
                            .query(
                                    buildQuery(contentSearchRequest, extra)
                            )
                            .from(contentSearchRequest.getOffset())
                            .size(contentSearchRequest.getLimit())
                            .sort(new FieldSortBuilder("lastModified").order(SortOrder.DESC))
                            .fetchSource(includes, excludes)))
                    .getHits().spliterator(), false)
                    .map(SearchHit::getSourceAsString)
                    .filter(Objects::nonNull);
        } catch (IOException e) {
            LOGGER.error("Failed to search", e);
            return Stream.empty();
        }
    }

    private QueryBuilder buildQuery(ContentSearchRequest contentSearchRequest, StateFields stateFields) {
        return QueryBuilders.boolQuery()
                .must(QueryBuilders.termsQuery("state", stateFields.getState()))
                .must(QueryBuilders.termsQuery("homeSection.state", stateFields.getHomeSection().getState()))
                ;
    }


    public void delete(Long id) throws IOException {
        restHighLevelClient.delete(new DeleteRequest(CONTENT_INDEX, ID_FIELD, String.valueOf(id)));
    }

    public Flux<String> findByIds(Set<Long> ids) {
        LOGGER.info("Multi getting ids: {}", ids);
        MultiGetRequest multiGetRequest = new MultiGetRequest();
        ids.forEach(id -> multiGetRequest.add(CONTENT_INDEX, ID_FIELD, String.valueOf(id)));
        return Flux.fromStream(() -> {
            try {
                return StreamSupport.stream(restHighLevelClient
                        .multiGet(multiGetRequest).spliterator(), false)
                        .map(MultiGetItemResponse::getResponse)
                        .map(GetResponse::getSourceAsString)
                        .filter(Objects::nonNull);
            } catch (IOException e) {
                LOGGER.error("Failed to search", e);
                return Stream.empty();
            }
        });
    }


    public List<Content> updateSection(Section section) {
        LOGGER.info("Updating section {} ", section);

        try {
            return StreamSupport.stream(restHighLevelClient.search(new SearchRequest(CONTENT_INDEX)
                    .source(new SearchSourceBuilder()
                            .query(QueryBuilders.termsQuery("homeSection.sectionId",
                                    String.valueOf(section.getSectionId())))))
                    .getHits().spliterator(), false)
                    .map(SearchHit::getId)
                    .map(id -> updateSection(id, section))
                    .filter(Objects::nonNull)
                    .map(result -> {
                        LOGGER.info("Content id {} updated with status {}", result.v1(), result.v2());
                        return new Content(Long.parseLong(result.v1()));
                    }).collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("Failed to search", e);
            return Collections.emptyList();
        }

    }

    private Tuple<String, RestStatus> updateSection(String id, Section section) {
        try {
            LOGGER.info("Updating content id: {} to section: {}", id, section);
            return Tuple.tuple(id, restHighLevelClient.update(new UpdateRequest(CONTENT_INDEX, ID_FIELD, id)
                    .doc(getSectionDocMap(section))).status());
        } catch (IOException e) {
            LOGGER.warn("Unable to update section on {}", id, e);
        }
        return null;
    }

    private Map<String, Map<String, String>> getSectionDocMap(Section section) {
        return objectMapper.convertValue(section, Map.class);
    }
}
