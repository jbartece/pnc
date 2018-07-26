/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rest.endpoint;

import org.jboss.pnc.coordinator.builder.DefaultBuildCoordinator;
import org.jboss.pnc.model.BuildConfiguration;
import org.jboss.pnc.model.User;
import org.jboss.pnc.rest.configuration.BuildConfigurationSupportedGenericParameters;
import org.jboss.pnc.rest.provider.BuildConfigurationProvider;
import org.jboss.pnc.rest.provider.BuildRecordProvider;
import org.jboss.pnc.rest.restmodel.BuildConfigurationRest;
import org.jboss.pnc.rest.restmodel.response.Singleton;
import org.jboss.pnc.rest.trigger.BuildTriggerer;
import org.jboss.pnc.rest.utils.EndpointAuthenticationProvider;
import org.jboss.pnc.rest.utils.HibernateLazyInitializer;
import org.jboss.pnc.rest.validation.exceptions.InvalidEntityException;
import org.jboss.pnc.spi.BuildOverrides;
import org.jboss.pnc.spi.coordinator.BuildSetTask;
import org.jboss.pnc.spi.coordinator.BuildTask;
import org.jboss.pnc.spi.datastore.Datastore;
import org.jboss.pnc.spi.datastore.repositories.BuildConfigurationRepository;
import org.jboss.pnc.spi.exception.BuildConflictException;
import org.jboss.pnc.spi.exception.CoreException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.common.util.RandomUtils.randInt;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 
 * @author Jakub Bartecek &lt;jbartece@redhat.com&gt;
 *
 */
public class BuildConfigurationEndpointTest {

    private static final int CURRENT_USER = randInt(1000, 100000);

    private static final String CUSTOM_PME_PARAMETERS = "CUSTOM_PME_PARAMETERS";

    @Mock
    private BuildConfigurationRepository buildConfigurationRepository;

    @Mock
    private Datastore datastore;

    @Mock
    private EndpointAuthenticationProvider authProvider;

    @Mock
    private DefaultBuildCoordinator defaultBuildCoordinator;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private BuildRecordProvider buildRecordProvider;

    @Mock
    HibernateLazyInitializer hibernateLazyInitializer;

    @Spy
    private BuildConfigurationSupportedGenericParameters supportedGenericParameters;

    @InjectMocks
    private BuildConfigurationProvider bcProvider = new BuildConfigurationProvider();

    @Spy
    @InjectMocks
    private BuildTriggerer buildTriggerer;

    private BuildConfigurationEndpoint bcEndpoint;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        bcEndpoint = new BuildConfigurationEndpoint(bcProvider, null, buildTriggerer, buildRecordProvider, null,
                authProvider, supportedGenericParameters);

        User user = mock(User.class);
        when(user.getId()).thenReturn(CURRENT_USER);
        when(authProvider.getCurrentUser(any())).thenReturn(user);

        when(uriInfo.getBaseUri()).thenReturn(URI.create("example.com"));
    }

    @Test
    public void testGetBCWithGenericParameters() {
        // given
        final String KEY = "KEY";
        final String VALUE = "VALUE";
        
        Map<String, String> genericParams = new HashMap<>();
        genericParams.put(KEY, VALUE);
        BuildConfiguration bcGiven = BuildConfiguration.Builder.newBuilder()
                .id(1)
                .genericParameters(genericParams)
                .build();
        when(buildConfigurationRepository.queryById(1)).thenReturn(bcGiven);

        // when
        Response response = bcEndpoint.getSpecific(1);
        

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        BuildConfigurationRest bcRest = (BuildConfigurationRest) 
            ((Singleton<BuildConfigurationRest>) response.getEntity()).getContent();

        Map<String, String> supportedParameters = bcRest.getGenericParameters();
        assertThat(supportedParameters).containsKey(KEY);
        assertThat(supportedParameters.get(KEY)).startsWith(VALUE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetPMEGenericParameterWithDescription() {
        // when
        Response response = bcEndpoint.getSupportedGenericParameters();

        // then
        assertThat(response.getStatus()).isEqualTo(200);

        Map<String, String> supportedParameters = (Map<String, String>) bcEndpoint
                .getSupportedGenericParameters().getEntity();

        assertThat(supportedParameters).containsKey(CUSTOM_PME_PARAMETERS);
        assertThat(supportedParameters.get(CUSTOM_PME_PARAMETERS)).startsWith("Additional");
    }

    @Test(expected = InvalidEntityException.class)
    public void invalidBuildOptionsRejectedTest() throws InvalidEntityException, MalformedURLException, CoreException, BuildConflictException {
        // when
        bcEndpoint.trigger(1, null, false, false, false, false, true, null);
    }

    @Test
    public void shouldTriggerBuildWithOverrides() throws Exception {
        // given
        // Override entities
        final String overriddenBuildScript = "mvn clean deploy; Overriden build script";
        final String overriddenScmRevision = "OverriddenTag";
        final Map<String, String> overriddenGenericParameters = new HashMap<>();
        overriddenGenericParameters.put(CUSTOM_PME_PARAMETERS, "Overriden PME parameters");

        BuildOverrides buildOverrides = new BuildOverrides(overriddenScmRevision, overriddenBuildScript, overriddenGenericParameters);

        // Original BuildConfiguration
        Map<String, String> genericParams = new HashMap<>();
        genericParams.put(CUSTOM_PME_PARAMETERS, "value");
        BuildConfiguration originalBuildConfiguration = BuildConfiguration.Builder.newBuilder()
                .id(1)
                .buildScript("mvn clean deploy")
                .scmRevision("master")
                .genericParameters(genericParams)
                .build();

        when(buildConfigurationRepository.queryById(1)).thenReturn(originalBuildConfiguration);
        when(hibernateLazyInitializer.initializeBuildConfigurationBeforeTriggeringIt(originalBuildConfiguration)).thenReturn(originalBuildConfiguration);

        // Mocking values to make the Spy BuildTriggerer working
        BuildSetTask mockedBuildSetTask = Mockito.mock(BuildSetTask.class);
        BuildTask mockedBuildTask = Mockito.mock(BuildTask.class);
        when(mockedBuildTask.getBuildConfiguration()).thenReturn(BuildConfiguration.Builder.newBuilder().id(1).build());

        Set<BuildTask> buildTasks = new HashSet<>();
        buildTasks.add(mockedBuildTask);
        when(mockedBuildSetTask.getBuildTasks()).thenReturn(buildTasks);

        // Capturing parameters
        ArgumentCaptor<BuildConfiguration> argumentCaptorBuildConfiguration = ArgumentCaptor.forClass(BuildConfiguration.class);
        when(defaultBuildCoordinator.build(argumentCaptorBuildConfiguration.capture(), any(), any())).thenReturn(mockedBuildSetTask);

        // when
        bcEndpoint.triggerWithOverrides(1, null, false, false, false, false, false, buildOverrides, uriInfo);

        // then
        BuildConfiguration capturedBuildConfiguration = argumentCaptorBuildConfiguration.getValue();

        assertEquals(overriddenScmRevision, capturedBuildConfiguration.getScmRevision());
        assertEquals(overriddenBuildScript, capturedBuildConfiguration.getBuildScript());

        assertEquals(1, capturedBuildConfiguration.getGenericParameters().size());
        assertEquals(overriddenGenericParameters.get(CUSTOM_PME_PARAMETERS), capturedBuildConfiguration.getGenericParameters().get(CUSTOM_PME_PARAMETERS));
    }

}
