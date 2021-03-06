package de.funkedigital.fuzo.contentservice.service;

import de.funkedigital.fuzo.contentservice.models.Content;
import de.funkedigital.fuzo.contentservice.models.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.validation.Valid;

@Service
@Validated
public class EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventService.class);

    private final Map<Event.DataType, Function<Event, List<Content>>> handlers = new HashMap<>();

    EventService(ArticleService articleService,
                 SectionService sectionService) {
        handlers.put(Event.DataType.ARTICLE, articleService::handleEvent);
        handlers.put(Event.DataType.SECTION, sectionService::handleEvent);
    }

    public List<Content> handleEvent(@Valid Event event) {
        return Optional.ofNullable(handlers.get(event.getDataType()))
                .map(eventMonoFunction -> eventMonoFunction.apply(event))
                .orElseGet(() -> {
                    LOGGER.warn("Event data type not supported: {}", event);
                    return Collections.emptyList();
                });
    }

}
