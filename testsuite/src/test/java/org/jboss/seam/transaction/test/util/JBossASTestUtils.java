/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.seam.transaction.test.util;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

/**
 * @author Stuart Douglas
 * @author Marek Schmidt
 */
public class JBossASTestUtils {
    /**
     * Creates a test archive with an empty beans.xml
     *
     * @return
     */
    public static WebArchive createTestArchive() {
        return createTestArchive(true);
    }

    public static WebArchive createTestArchive(boolean includeEmptyBeansXml) {
        WebArchive war = ShrinkWrap.createDomain().getArchiveFactory().create(WebArchive.class, "test.war");
        war.addAsLibraries(
                DependencyResolvers.use(MavenDependencyResolver.class)
                .loadReposFromPom("pom.xml")
                .artifact("org.jboss.solder:solder-impl")
                .resolveAs(JavaArchive.class)
                );

        war.addAsLibraries(
                ShrinkWrap.create(
                        ZipImporter.class, "seam-transaction-api.jar")
                            .importFrom(new File("../api/target/seam-transaction-api.jar"))
                            .as(JavaArchive.class),

                ShrinkWrap.create(
                        ZipImporter.class, "seam-transaction.jar")
                            .importFrom(new File("../impl/target/seam-transaction.jar"))
                            .as(JavaArchive.class));

        if (includeEmptyBeansXml) {
            war.addAsWebInfResource(new ByteArrayAsset(new byte[0]), "beans.xml");
        }

        // Disabling the ServletRequestListener
        war.addAsWebInfResource(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<web-app xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
                "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\" version=\"3.0\">\n" +
                "    <context-param>\n" +
                "        <param-name>org.jboss.seam.transaction.disableListener</param-name>\n" +
                "        <param-value>true</param-value>\n" +
                "    </context-param>\n" +
                "</web-app>"), "web.xml");
        return war;
    }

}
