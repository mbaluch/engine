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
package io.lumeer.engine.rest;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.constraint.ConstraintManager;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.ConfigurationFacade;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Whispers to user any possibilities that can be entered as input.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Path("/whisper")
public class WhisperService {

   @Inject
   private DataStorage dataStorage;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private ConstraintManager constraintManager;

   @Inject
   private ConfigurationFacade configurationFacade;

   private Locale locale = Locale.getDefault();

   @PostConstruct
   public void initLocale() {
      locale = Locale.forLanguageTag(configurationFacade.getConfigurationString(LumeerConst.USER_LOCALE_PROPERTY).orElse("en-US"));
   }

   @GET
   @Path("/collection/{collectionName}")
   @Produces(MediaType.APPLICATION_JSON)
   public Set<String> getPossibleCollectionNames(@PathParam("collectionName") final String collectionName) {
      if (collectionName == null || collectionName.isEmpty()) {
         // return user names of all user collections
         return Collections.emptySet();
      } else {
         // return user names of all user collections starting with the collectionName ignoring case
         return Collections.emptySet();
         //return dataStorage.getAllCollections().stream().filter(name -> name.startsWith(prefix)).collect(Collectors.toSet());

      }
   }

   @GET
   @Path("/collection")
   @Produces(MediaType.APPLICATION_JSON)
   public Set<String> getPossibleCollectionNames() {
      return getPossibleCollectionNames(null);
   }

   @GET
   @Path("/collection/{collectionName}/attr/{userInput}")
   @Produces(MediaType.APPLICATION_JSON)
   public Set<String> getPossibleCollectionAttributeNames(@PathParam("collectionName") final String userCollectionName, @PathParam("userInput") final String userInput) {
      // returns empty set if user collection name does not exsits
      // returns all collection attributes (except for meta data) if userInput is null or empty
      // return all collections attributes (except for meta data) starting with the userInput ignoring case

      return Collections.emptySet();
   }

   @GET
   @Path("/collection/{collectionName}/attr")
   @Produces(MediaType.APPLICATION_JSON)
   public Set<String> getPossibleCollectionAttributeNames(@PathParam("collectionName") final String userCollectionName) {
      return getPossibleCollectionAttributeNames(userCollectionName, null);
   }

   /**
    * Gets available names of constraint prefixes.
    *
    * @param constraintName
    *       Already written part of the constraint prefix.
    * @return Set of available constraint prefix names according to the already entered part.
    */
   @GET
   @Path("/constraint/{constraintName}")
   @Produces(MediaType.APPLICATION_JSON)
   public Set<String> getPossibleConstraintNamePrefixes(@PathParam("constraintName") final String constraintName) {
      System.out.println("prefixes.");
      if (constraintName != null && !constraintName.isEmpty()) {
         return constraintManager.getRegisteredPrefixes().stream().filter(prefix -> prefix.toLowerCase(locale).startsWith(constraintName.toLowerCase(locale))).collect(Collectors.toSet());
      }

      return constraintManager.getRegisteredPrefixes();
   }

   @GET
   @Path("/constraint")
   @Produces(MediaType.APPLICATION_JSON)
   public Set<String>  getPossibleConstraintNamePrefixes() {
      return getPossibleConstraintNamePrefixes(null);
   }

   /**
    * Gets available parameter values for the given constraint.
    *
    * @param constraintName
    *       Name of the constraint.
    * @param constraintParam
    *       Already written part of the constraint parameter.
    * @return Set of available constraint parameters based on the already entered part.
    */
   @GET
   @Path("/constraint/{constraintName}/param/{constraintParam}")
   @Produces(MediaType.APPLICATION_JSON)
   public Set<String> getPossibleConstraintNameParameters(@PathParam("constraintName") final String constraintName, @PathParam("constraintParam") final String constraintParam) {
      System.out.println("prefixes: " + constraintParam);
      if (constraintParam != null && !constraintParam.isEmpty()) {
         return constraintManager.getConstraintParameterSuggestions(constraintName).stream().filter(
               suggestion -> suggestion.toLowerCase(locale).startsWith(constraintParam.toLowerCase(locale))).collect(Collectors.toSet());
      }

      return constraintManager.getConstraintParameterSuggestions(constraintName);
   }

   @GET
   @Path("/constraint/{constraintName}/param")
   @Produces(MediaType.APPLICATION_JSON)
   public Set<String> getPossibleConstraintNameParameters(@PathParam("constraintName") final String constraintName) {
      return getPossibleConstraintNameParameters(constraintName, null);
   }

}
