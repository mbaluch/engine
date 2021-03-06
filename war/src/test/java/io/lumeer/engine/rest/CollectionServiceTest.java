/*
 * -----------------------------------------------------------------------\
 * Lummer
 *  
 * Copyright (C) 2016 the original author or authors.
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
package io.lumeer.engine.rest;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.DocumentFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.rest.dao.AccessRightsDao;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Tests the collection service while deployed on the application server.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
public class CollectionServiceTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "CollectionServiceTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties");
   }

   private final String TARGET_URI = "http://localhost:8080";
   private final String PATH_PREFIX = "CollectionServiceTest/rest/collections/";

   private final String COLLECTION_GET_ALL_COLLECTIONS_1 = "CollectionServiceCollectionGetAllCollections1";
   private final String COLLECTION_GET_ALL_COLLECTIONS_2 = "CollectionServiceCollectionGetAllCollections2";
   private final String COLLECTION_CREATE_COLLECTION = "CollectionServiceCollectionCreateCollection";
   private final String COLLECTION_DROP_COLLECTION = "CollectionServiceCollectionDropCollection";
   private final String COLLECTION_RENAME_ATTRIBUTE = "CollectionServiceCollectionRenameAttribute";
   private final String COLLECTION_DROP_ATTRIBUTE = "CollectionServiceCollectionDropAttribute";
   private final String COLLECTION_OPTION_SEARCH = "CollectionServiceCollectionOptionSearch";
   private final String COLLECTION_QUERY_SEARCH = "CollectionServiceCollectionQuerySearch";
   private final String COLLECTION_ADD_COLLECTION_METADATA = "CollectionServiceCollectionAddCollectionMetadata";
   private final String COLLECTION_READ_COLLECTION_METADATA = "CollectionServiceCollectionReadCollectionMetadata";
   private final String COLLECTION_UPDATE_COLLECTION_METADATA = "CollectionServiceCollectionUpdateCollectionMetadata";
   private final String COLLECTION_READ_COLLECTION_ATTRIBUTES = "CollectionServiceCollectionReadCollectionAttributes";
   private final String COLLECTION_SET_AND_READ_ATTRIBUTE_TYPE = "CollectionServiceCollectionSetAndReadAttributeType";
   private final String COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT = "CollectionServiceCollectionSetReadAndDropAttributeConstraint";
   private final String COLLECTION_READ_ACCESS_RIGHTS = "CollectionServiceCollectionReadAccessRights";
   private final String COLLECTION_UPDATE_ACCESS_RIGHTS = "CollectionServiceCollectionuUpdateAccessRights";

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private CollectionService collectionService;

   @Inject
   private DataStorage dataStorage;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private UserFacade userFacade;

   @Test
   public void testRegister() throws Exception {
      Assert.assertNotNull(collectionService);
   }

   @Test
   public void testRestClient() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();
      // the path prefix '/lumeer-engine/' does not work properly in test classes
      Response response = client.target(TARGET_URI).path("/lumeer-engine/rest/collections/").request(MediaType.APPLICATION_JSON_TYPE).buildGet().invoke();
   }

   @Test
   public void testGetAllCollections() throws Exception {
      setUpCollections(COLLECTION_GET_ALL_COLLECTIONS_1);
      setUpCollections(COLLECTION_GET_ALL_COLLECTIONS_2);
      collectionFacade.createCollection(COLLECTION_GET_ALL_COLLECTIONS_1);
      collectionFacade.createCollection(COLLECTION_GET_ALL_COLLECTIONS_2);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();

      ArrayList<String> collections = response.readEntity(ArrayList.class);
      Assert.assertTrue(collections.equals(new ArrayList<String>(collectionFacade.getAllCollections().values())) && response.getStatus() == Response.Status.OK.getStatusCode());
      response.close();
      client.close();
   }

   @Test
   public void testCreateCollection() throws Exception {
      setUpCollections(COLLECTION_CREATE_COLLECTION);

      // #1 first time collection creation, status code = 200
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_CREATE_COLLECTION).request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      String internalName = response.readEntity(String.class);
      Assert.assertTrue(dataStorage.hasCollection(internalName) && response.getStatus() == Response.Status.OK.getStatusCode() && internalName.equals(getInternalName(COLLECTION_CREATE_COLLECTION)));
      response.close();

      // #2 collection already exists, status code = 400
      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_CREATE_COLLECTION).request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      Assert.assertTrue(dataStorage.hasCollection(internalName) && response2.getStatus() == Response.Status.BAD_REQUEST.getStatusCode());
      response2.close();

      client.close();
   }

   @Test
   public void testDropCollection() throws Exception {
      setUpCollections(COLLECTION_DROP_COLLECTION);
      final Client client = ClientBuilder.newBuilder().build();

      // #1 nothing to delete, status code = 404
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_DROP_COLLECTION).request().buildDelete().invoke();
      Assert.assertTrue(!dataStorage.hasCollection(getInternalName(COLLECTION_DROP_COLLECTION))
            && response.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
      response.close();

      // #2 collection exists, ready to delete, status code = 204
      String internalCollectionName = collectionFacade.createCollection(COLLECTION_DROP_COLLECTION);
      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_DROP_COLLECTION).request().buildDelete().invoke();

      boolean responseStatus = (response2.getStatus() == Response.Status.NO_CONTENT.getStatusCode());
      List<DataDocument> collections = dataStorage.run(new DataDocument("listCollections", 1));
      List<String> collectionNames = new ArrayList<>();
      collections.forEach(document -> collectionNames.add(document.getString("name")));

      Assert.assertTrue(responseStatus);
      Assert.assertFalse(collectionNames.contains(internalCollectionName));
      response2.close();

      client.close();
   }

   @Test
   public void testRenameAttribute() throws Exception {
      setUpCollections(COLLECTION_RENAME_ATTRIBUTE);

      final String oldAttributeName = "testAttribute";
      final String newAttributeName = "testNewAttribute";

      // #1 the given collection does not exist, status code = 400 or 404
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_RENAME_ATTRIBUTE + "/attributes/" + oldAttributeName + "/rename/" + newAttributeName).request().buildPut(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      Assert.assertTrue(response.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
      response.close();

      // #2 the given collection and attribute exists, ready to rename the attribute, status code = 204
      collectionFacade.createCollection(COLLECTION_RENAME_ATTRIBUTE);
      collectionMetadataFacade.addOrIncrementAttribute(getInternalName(COLLECTION_RENAME_ATTRIBUTE), oldAttributeName);

      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_RENAME_ATTRIBUTE + "/attributes/" + oldAttributeName + "/rename/" + newAttributeName).request().buildPut(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      List<String> attributeNames = collectionMetadataFacade.getCollectionAttributesNames(getInternalName(COLLECTION_RENAME_ATTRIBUTE));
      boolean containsNewAttribute = attributeNames.contains(newAttributeName);
      boolean containsOldAttribute = attributeNames.contains(oldAttributeName);
      Assert.assertTrue(response2.getStatus() == Response.Status.NO_CONTENT.getStatusCode()
            && containsNewAttribute && !containsOldAttribute);
      response2.close();

      client.close();
   }

   @Test
   public void testDropAttribute() throws Exception {
      setUpCollections(COLLECTION_DROP_ATTRIBUTE);
      final String attributeName = "testAttributeToAdd";

      // #1 the given collection does not exist, status code = 404
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_DROP_ATTRIBUTE + "/attributes/" + attributeName).request().buildDelete().invoke();
      Assert.assertTrue(response.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
      response.close();

      // #2 the given collection and attribute exists, ready to drop the attribute, status code = 204
      collectionFacade.createCollection(COLLECTION_DROP_ATTRIBUTE);
      collectionMetadataFacade.addOrIncrementAttribute(getInternalName(COLLECTION_DROP_ATTRIBUTE), attributeName);
      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_DROP_ATTRIBUTE + "/attributes/" + attributeName).request().buildDelete().invoke();
      List<String> attributeNames = collectionMetadataFacade.getCollectionAttributesNames(getInternalName(COLLECTION_DROP_ATTRIBUTE));
      boolean containsKey = attributeNames.contains(attributeName);
      Assert.assertTrue(response2.getStatus() == Response.Status.NO_CONTENT.getStatusCode()
            && !containsKey);
      response2.close();

      client.close();
   }

   @Test
   public void testOptionSearch() throws Exception {
      setUpCollections(COLLECTION_OPTION_SEARCH);
      final Client client = ClientBuilder.newBuilder().build();
      final int limit = 5;

      collectionFacade.createCollection(COLLECTION_OPTION_SEARCH);
      createDummyEntries(COLLECTION_OPTION_SEARCH);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_OPTION_SEARCH + "/search/")
                                .queryParam("filter", null)
                                .queryParam("sort", null)
                                .queryParam("skip", 0)
                                .queryParam("limit", limit)
                                .request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      List<DataDocument> searchedDocuments = response.readEntity(ArrayList.class);
      Assert.assertTrue(response.getStatus() == Response.Status.OK.getStatusCode() && searchedDocuments.size() == limit);
      response.close();

      client.close();
   }

   @Test
   public void testQuerySearch() throws Exception {
      setUpCollections(COLLECTION_QUERY_SEARCH);
      final Client client = ClientBuilder.newBuilder().build();
      //final String query = "{find:" + "\"" + COLLECTION_QUERY_SEARCH + "\"" + ", limit : 5}";
      final String query = "find: \"" + getInternalName(COLLECTION_QUERY_SEARCH) + "\""; // TODO: correct query representation?

      collectionFacade.createCollection(COLLECTION_QUERY_SEARCH);
      createDummyEntries(COLLECTION_QUERY_SEARCH);

      /*Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_QUERY_SEARCH + "/run/").queryParam("query", query).request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      List<DataDocument> searchedDocuments = response.readEntity(ArrayList.class);
      Assert.assertTrue(response.getStatus() == Response.Status.OK.getStatusCode());
      response.close();*/

      client.close();
   }

   @Test
   public void testAddCollectionMetadata() throws Exception {
      setUpCollections(COLLECTION_ADD_COLLECTION_METADATA);
      final Client client = ClientBuilder.newBuilder().build();

      String attributeName = "metaAttribute";
      DataDocument value = new DataDocument("columnSize", 100);
      collectionFacade.createCollection(COLLECTION_ADD_COLLECTION_METADATA);
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_ADD_COLLECTION_METADATA + "/meta/" + attributeName).request(MediaType.APPLICATION_JSON).buildPost(Entity.entity(value, MediaType.APPLICATION_JSON)).invoke();
      List<DataDocument> metadata = collectionFacade.readCollectionMetadata(getInternalName(COLLECTION_ADD_COLLECTION_METADATA));
      DataDocument readMetaDoc = (DataDocument) metadata.get(3).get(attributeName);
      Assert.assertTrue(response.getStatus() == Response.Status.NO_CONTENT.getStatusCode() && readMetaDoc.equals(value));
      response.close();

      client.close();
   }

   @Test
   public void testReadCollectionMetadata() throws Exception {
      setUpCollections(COLLECTION_READ_COLLECTION_METADATA);
      final Client client = ClientBuilder.newBuilder().build();

      // #1 no metadata collection exists, status code = 404
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_READ_COLLECTION_METADATA + "/meta/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      Assert.assertTrue(response.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
      response.close();

      // #2 the metadata collection of the given collection exists
      collectionFacade.createCollection(COLLECTION_READ_COLLECTION_METADATA);
      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_READ_COLLECTION_METADATA + "/meta/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      ArrayList<DataDocument> collectionMetadata = response2.readEntity(ArrayList.class);
      List<DataDocument> metadata = collectionFacade.readCollectionMetadata(getInternalName(COLLECTION_READ_COLLECTION_METADATA));
      Assert.assertTrue(response2.getStatus() == Response.Status.OK.getStatusCode()
            && collectionMetadata.equals(metadata));
      response2.close();

      client.close();
   }

   @Test
   public void testUpdateCollectionMetadata() throws Exception {
      setUpCollections(COLLECTION_UPDATE_COLLECTION_METADATA);
      final Client client = ClientBuilder.newBuilder().build();
      final String columnSizeAttributeName = "columnSize";
      final int value = 100;
      final int updatedValue = 500;

      collectionFacade.createCollection(COLLECTION_UPDATE_COLLECTION_METADATA);
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_UPDATE_COLLECTION_METADATA + "/meta/" + columnSizeAttributeName).request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(value, MediaType.APPLICATION_JSON)).invoke();
      List<DataDocument> metadata = collectionFacade.readCollectionMetadata(getInternalName(COLLECTION_UPDATE_COLLECTION_METADATA));
      int readValue = metadata.get(3).getInteger(columnSizeAttributeName);
      Assert.assertTrue(response.getStatus() == Response.Status.NO_CONTENT.getStatusCode() && readValue == value);
      response.close();

      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_UPDATE_COLLECTION_METADATA + "/meta/" + columnSizeAttributeName).request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(updatedValue, MediaType.APPLICATION_JSON)).invoke();
      List<DataDocument> updatedMetadata = collectionFacade.readCollectionMetadata(getInternalName(COLLECTION_UPDATE_COLLECTION_METADATA));
      int readUpdatedValue = updatedMetadata.get(3).getInteger(columnSizeAttributeName);
      Assert.assertTrue(response2.getStatus() == Response.Status.NO_CONTENT.getStatusCode() && readUpdatedValue == updatedValue);
      response2.close();

      client.close();
   }

   @Test
   public void testReadCollectionAttributes() throws Exception {
      setUpCollections(COLLECTION_READ_COLLECTION_ATTRIBUTES);

      final Client client = ClientBuilder.newBuilder().build();
      final String dummyAttribute1 = "dummyAttribute1";
      final String dummyAttribute2 = "dummyAttribute2";
      final String dummyAttribute3 = "dummyAttribute3";

      // #1 if the given collection does not exist, status code = 404
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_READ_COLLECTION_ATTRIBUTES + "/attributes/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      Assert.assertTrue(response.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
      response.close();

      // #2 the given collection and attributes exists, status code = 200
      collectionFacade.createCollection(COLLECTION_READ_COLLECTION_ATTRIBUTES);
      collectionMetadataFacade.addOrIncrementAttribute(getInternalName(COLLECTION_READ_COLLECTION_ATTRIBUTES), dummyAttribute1);
      collectionMetadataFacade.addOrIncrementAttribute(getInternalName(COLLECTION_READ_COLLECTION_ATTRIBUTES), dummyAttribute2);
      collectionMetadataFacade.addOrIncrementAttribute(getInternalName(COLLECTION_READ_COLLECTION_ATTRIBUTES), dummyAttribute3);

      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_READ_COLLECTION_ATTRIBUTES + "/attributes/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      ArrayList<String> collectionAttributes = response2.readEntity(ArrayList.class);
      Assert.assertTrue(response2.getStatus() == Response.Status.OK.getStatusCode()
            && collectionAttributes.equals(collectionFacade.readCollectionAttributes(getInternalName(COLLECTION_READ_COLLECTION_ATTRIBUTES))));
      response2.close();

      client.close();
   }

   /*@Test
   public void testReadAccessRights() throws Exception {
      setUpCollections(COLLECTION_READ_ACCESS_RIGHTS);
      final Client client = ClientBuilder.newBuilder().build();

      collectionFacade.createCollection(COLLECTION_READ_ACCESS_RIGHTS);
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_READ_ACCESS_RIGHTS + "/rights").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      DataDocument accessRights = response.readEntity(DataDocument.class);
      Assert.assertTrue(response.getStatus() == Response.Status.OK.getStatusCode() && !accessRights.isEmpty());
      response.close();
      client.close();
   }*/

   @Test
   public void testReadAccessRights() throws Exception {
      setUpCollections(COLLECTION_READ_ACCESS_RIGHTS);
      final Client client = ClientBuilder.newBuilder().build();
      final String user = userFacade.getUserEmail();
      final AccessRightsDao DEFAULT_ACCESS_RIGHT = new AccessRightsDao(true, true, true, user);

      collectionFacade.createCollection(COLLECTION_READ_ACCESS_RIGHTS);
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_READ_ACCESS_RIGHTS + "/rights").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      List<AccessRightsDao> rights = response.readEntity(new GenericType<List<AccessRightsDao>>() {
      });
      AccessRightsDao readRights = rights.get(0);
      Assert.assertTrue(response.getStatus() == Response.Status.OK.getStatusCode()
            && readRights.isWrite() == DEFAULT_ACCESS_RIGHT.isWrite()
            && readRights.isRead() == DEFAULT_ACCESS_RIGHT.isRead()
            && readRights.isExecute() == DEFAULT_ACCESS_RIGHT.isExecute()
            && readRights.getUserName().equals(DEFAULT_ACCESS_RIGHT.getUserName()));
      response.close();
      client.close();
   }

   @Test
   public void testUpdateAccessRights() throws Exception {
      setUpCollections(COLLECTION_UPDATE_ACCESS_RIGHTS);
      final Client client = ClientBuilder.newBuilder().build();
      final String user = userFacade.getUserEmail();
      final AccessRightsDao accessRights = new AccessRightsDao(true, true, false, user);

      collectionFacade.createCollection(COLLECTION_UPDATE_ACCESS_RIGHTS);
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_UPDATE_ACCESS_RIGHTS + "/rights").request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(accessRights, MediaType.APPLICATION_JSON)).invoke();
      DataDocument metadata = collectionFacade.readCollectionMetadata(getInternalName(COLLECTION_UPDATE_ACCESS_RIGHTS)).get(1);
      AccessRightsDao readAccessRights = securityFacade.getDao(collectionMetadataFacade.collectionMetadataCollectionName(getInternalName(COLLECTION_UPDATE_ACCESS_RIGHTS)), metadata.getId(), user);
      Assert.assertTrue(response.getStatus() == Response.Status.NO_CONTENT.getStatusCode() && readAccessRights.isWrite() && readAccessRights.isRead() && !readAccessRights.isExecute());
      response.close();
      client.close();
   }

   @Test
   public void testSetAndReadAttributeType() throws Exception {
      setUpCollections(COLLECTION_SET_AND_READ_ATTRIBUTE_TYPE);

      final Client client = ClientBuilder.newBuilder().build();
      final String attributeName = "dummyAttribute";
      final String newType = LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_VALUES.get(0);

      collectionFacade.createCollection(COLLECTION_SET_AND_READ_ATTRIBUTE_TYPE);
      collectionMetadataFacade.addOrIncrementAttribute(getInternalName(COLLECTION_SET_AND_READ_ATTRIBUTE_TYPE), attributeName);

      // set attribute type
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_SET_AND_READ_ATTRIBUTE_TYPE + "/attributes/" + attributeName + "/types/" + newType).request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      boolean wasSuccessful = response.readEntity(Boolean.class);
      Assert.assertTrue(response.getStatus() == Response.Status.OK.getStatusCode()
            && wasSuccessful);
      response.close();

      // read attribute type
      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_SET_AND_READ_ATTRIBUTE_TYPE + "/attributes/" + attributeName + "/types").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      String attributeType = response2.readEntity(String.class);
      Assert.assertTrue(response2.getStatus() == Response.Status.OK.getStatusCode()
            && attributeType.equals(newType));
      response2.close();

      client.close();
   }

   @Test
   public void testSetReadAndDropAttributeConstraint() throws Exception {
      setUpCollections(COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT);

      final Client client = ClientBuilder.newBuilder().build();
      final String constraintConfiguration = "case:lower";
      final String attributeName = "dummyAttributeName";

      collectionFacade.createCollection(COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT);
      collectionMetadataFacade.addOrIncrementAttribute(getInternalName(COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT), attributeName);

      // set attribute constraint
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT + "/attributes/" + attributeName + "/constraints").request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(constraintConfiguration, MediaType.APPLICATION_JSON)).invoke();
      Assert.assertTrue(response.getStatus() == Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      // read attribute constraint
      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT + "/attributes/" + attributeName + "/constraints").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      List<String> constraints = response2.readEntity(ArrayList.class);
      Assert.assertTrue(response2.getStatus() == Response.Status.OK.getStatusCode()
            && constraints.equals(collectionMetadataFacade.getAttributeConstraintsConfigurations(getInternalName(COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT), attributeName)));
      response2.close();

      // drop attribute constraint
      Response response3 = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT + "/attributes/" + attributeName + "/constraints").request(MediaType.APPLICATION_JSON).build("DELETE", Entity.json(constraintConfiguration)).invoke();
      Assert.assertTrue(response3.getStatus() == Response.Status.NO_CONTENT.getStatusCode()
            && collectionMetadataFacade.getAttributeConstraintsConfigurations(getInternalName(COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT), attributeName).size() == 0);
      response3.close();

      client.close();
   }

   private void createDummyEntries(final String collectionName) throws DbException, InvalidConstraintException {
      for (int i = 0; i < 10; i++) {
         documentFacade.createDocument(getInternalName(collectionName), new DataDocument("dummyAttribute", i));
      }
   }

   private void setUpCollections(final String collectionName) throws DbException {
      if (dataStorage.hasCollection(getInternalName(collectionName))) {
         collectionFacade.dropCollection(getInternalName(collectionName));
      }
   }

   private String getInternalName(final String collectionOriginalName) {
      return "collection." + collectionOriginalName.toLowerCase() + "_0";
   }
}
