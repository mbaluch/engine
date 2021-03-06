/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.controller;

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.Query;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.List;
import javax.inject.Inject;

/**
 * @author <a href="kubedo8@gmail.com">Jakub Rodák</a>
 */
public class SearchFacadeTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "SearchFacadeTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties");
   }

   private final String COLLECTION_SEARCH = "collectionSearch";
   private final String COLLECTION_SEARCH_RAW = "collectionSearchRaw";
   private final String COLLECTION_QUERY = "collectionQuery";

   @Inject
   private SearchFacade searchFacade;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private DataStorage dataStorage;

   @Test
   public void testSearch() throws Exception {
      setUpCollection(COLLECTION_SEARCH);

      for (int i = 0; i < 20; i++) {
         DataDocument insertedDocument = new DataDocument();
         documentFacade.createDocument(COLLECTION_SEARCH, insertedDocument);
      }

      List<DataDocument> searchDocuments = searchFacade.search(COLLECTION_SEARCH, null, null, 5, 5);

      Assert.assertEquals(searchDocuments.size(), 5);
   }

   @Test
   public void testRawSearch() throws Exception {
      setUpCollection(COLLECTION_SEARCH_RAW);

      for (int i = 0; i < 20; i++) {
         DataDocument insertedDocument = new DataDocument();
         documentFacade.createDocument(COLLECTION_SEARCH_RAW, insertedDocument);
      }

      String query = "{find: \"" + COLLECTION_SEARCH_RAW + "\"}";

      List<DataDocument> searchDocuments = searchFacade.search(query);

      // run() method returns 101 entries due to it is a default value of "batchSize" query key
      Assert.assertEquals(searchDocuments.size(), 20);
   }

   @Test
   public void testQuery() throws Exception {
      setUpCollection(COLLECTION_QUERY);

      DataDocument d1 = new DataDocument();

      //dataStorage.createDocument(COLLECTION_QUERY);

      final Query q = new Query();
   }

   private void setUpCollection(final String collection) {
      dataStorage.dropCollection(collection);
      dataStorage.dropCollection(collectionMetadataFacade.collectionMetadataCollectionName(collection));
      dataStorage.createCollection(collection);
      dataStorage.createCollection(collectionMetadataFacade.collectionMetadataCollectionName(collection));
   }

}
