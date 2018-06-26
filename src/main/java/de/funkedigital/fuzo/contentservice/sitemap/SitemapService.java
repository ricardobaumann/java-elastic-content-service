
package de.funkedigital.fuzo.contentservice.sitemap;


import de.funkedigital.fuzo.contentservice.models.ContentSearchRequest;
import de.funkedigital.fuzo.contentservice.repo.ContentRepo;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class SitemapService {

    private ContentRepo contentRepo;
    private String publicationName;


    public SitemapService(ContentRepo contentRepo, String publicationName) {
        this.contentRepo = contentRepo;
        this.publicationName = publicationName;
    }


    public void fetchLatestThousandContents() {

        fetchLatestThousandNews();
    }


    private void fetchLatestThousandNews() {

        ContentSearchRequest contentSearchRequest = new ContentSearchRequest(1000, publicationName);

        Set<String> contetTypeSet=new HashSet<>();
        contetTypeSet.add("news");
        contetTypeSet.add("opinion");
        contetTypeSet.add("printimport");
        contetTypeSet.add("recipe");

        contentSearchRequest.setContentTypeSet(contetTypeSet);




    }

}