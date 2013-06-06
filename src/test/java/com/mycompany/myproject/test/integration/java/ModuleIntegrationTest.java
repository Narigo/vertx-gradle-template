package com.mycompany.myproject.test.integration.java;/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

import static org.vertx.testtools.VertxAssert.*;

/**
 * Example Java integration test that deploys the module that this project builds.
 *
 * Quite often in integration tests you want to deploy the same module for all tests and you don't want tests
 * to start before the module has been deployed.
 *
 * This test demonstrates how to do that.
 */
public class ModuleIntegrationTest extends TestVerticle {

  @Test
  public void testPing() {
    container.logger().info("in testPing()");
    vertx.eventBus().send("ping-address", "ping!", new Handler<Message<String>>() {
      @Override
      public void handle(Message<String> reply) {
        assertEquals("pong!", reply.body());

        /*
        If we get here, the test is complete
        You must always call `testComplete()` at the end. Remember that testing is *asynchronous* so
        we cannot assume the test is complete by the time the test method has finished executing like
        in standard synchronous tests
        */
        testComplete();
      }
    });
  }

  @Test
  public void testMongo() {
    System.out.println("send something to vertx.mongopersistor");
    vertx.eventBus().send("vertx.mongopersistor", new JsonObject().putString("action", "unknownAction123"),
        new Handler<Message<JsonObject>>() {
          @Override
          public void handle(Message<JsonObject> event) {
            System.out.println("vertx.mongopersistor: " + event.body().encode());
            assertEquals("should get an error", event.body().getString("status", "error"));
            testComplete();
          }
        });

    System.out.println("send something to hello-mongo");
    vertx.eventBus().send("hello-mongo", new JsonObject().putString("action", "unknownAction123"),
        new Handler<Message<JsonObject>>() {
          @Override
          public void handle(Message<JsonObject> event) {
            System.out.println("received from hello-mongo: " + event.body().encode());
            assertEquals("should get an error", event.body().getString("status", "error"));
            testComplete();
          }
        });
  }

  @Test
  public void testSomethingElse() {
    // Whatever
    testComplete();
  }


  @Override
  public void start() {
    // Make sure we call initialize() - this sets up the assert stuff so assert functionality works correctly
    initialize();
    // Deploy the module - the System property `vertx.modulename` will contain the name of the module so you
    // don't have to hardecode it in your tests
    container.deployModule(System.getProperty("vertx.modulename"), new AsyncResultHandler<String>() {
      @Override
      public void handle(AsyncResult<String> asyncResultMod) {
        if (asyncResultMod.failed()) {
          fail("did not work :(");
        }
        container.deployModule("io.vertx~mod-mongo-persistor~2.0.0-beta3-SNAPSHOT",
            new JsonObject().putString("address", "hello-mongo"), new AsyncResultHandler<String>() {
              @Override
              public void handle(AsyncResult<String> asyncResult) {
                if (asyncResult.failed()) {
                  fail("did not work to deploy mongo persistor :(");
                }
                System.out.println("deployed mongo");
                // Deployment is asynchronous and this this handler will be
                // called when it's complete (or failed)
                assertTrue(asyncResult.succeeded());
                assertNotNull("deploymentID should not be null", asyncResult.result());
                // If deployed correctly then start the tests!
                startTests();
              }
            });
      }
    });
  }

}
