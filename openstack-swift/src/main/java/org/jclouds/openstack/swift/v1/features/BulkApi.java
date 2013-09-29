/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.openstack.swift.v1.features;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.net.UrlEscapers.urlFragmentEscaper;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.jclouds.http.HttpRequest;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.openstack.keystone.v2_0.filters.AuthenticateRequest;
import org.jclouds.openstack.swift.v1.domain.BulkDeleteResponse;
import org.jclouds.openstack.swift.v1.domain.ExtractArchiveResponse;
import org.jclouds.openstack.swift.v1.features.ObjectApi.SetPayload;
import org.jclouds.rest.Binder;
import org.jclouds.rest.annotations.BinderParam;
import org.jclouds.rest.annotations.QueryParams;
import org.jclouds.rest.annotations.RequestFilters;

import com.google.common.base.Joiner;

/**
 * Provides access to the Swift Bulk API.
 * 
 * <h3>Note</h3>
 * 
 * As of the Grizzly release, these operations occur <a
 * href="https://blueprints.launchpad.net/swift/+spec/concurrent-bulk">serially
 * on the backend</a>.
 * 
 * @see <a
 *      href="http://docs.openstack.org/developer/swift/misc.html#module-swift.common.middleware.bulk">
 *      Swift Bulk Middleware</a>
 */
@RequestFilters(AuthenticateRequest.class)
@Consumes(APPLICATION_JSON)
public interface BulkApi {

   /**
    * Extracts a tar archive at the path specified as {@code path}.
    * 
    * @param path
    *           path to extract under, if not empty string.
    * @param tar
    *           valid tar archive
    * @param format
    *           one of {@code tar}, {@code tar.gz}, or {@code tar.bz2}
    * 
    * @return {@link BulkDeleteResponse#errors()} are empty on success.
    */
   @Named("ExtractArchive")
   @PUT
   @Path("/{path}")
   ExtractArchiveResponse extractArchive(@PathParam("path") String path,
         @BinderParam(SetPayload.class) Payload payload, @QueryParam("extract-archive") String format);

   /**
    * Deletes multiple objects or containers, if present.
    * 
    * @param paths
    *           format of {@code container}, for an empty container, or
    *           {@code container/object} for an object.
    * 
    * @return {@link BulkDeleteResponse#errors()} are empty on success.
    */
   @Named("BulkDelete")
   @DELETE
   @Path("/")
   @QueryParams(keys = "bulk-delete")
   BulkDeleteResponse bulkDelete(@BinderParam(UrlEncodeAndJoinOnNewline.class) Iterable<String> paths);

   // NOTE: this cannot be tested on MWS and is also brittle, as it relies on
   // sending a body on DELETE.
   // https://bugs.launchpad.net/swift/+bug/1232787
   static class UrlEncodeAndJoinOnNewline implements Binder {
      @SuppressWarnings("unchecked")
      @Override
      public <R extends HttpRequest> R bindToRequest(R request, Object input) {
         String encodedAndNewlineDelimited = Joiner.on('\n').join(
               transform(Iterable.class.cast(input), urlFragmentEscaper().asFunction()));
         Payload payload = Payloads.newStringPayload(encodedAndNewlineDelimited);
         payload.getContentMetadata().setContentType(TEXT_PLAIN);
         return (R) request.toBuilder().payload(payload).build();
      }
   }
}