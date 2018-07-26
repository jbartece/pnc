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
package org.jboss.pnc.spi;

import lombok.*;
import org.jboss.pnc.model.BuildConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Class containing overrides of values stored in BuildConfiguration in database used to customize builds
 * without a need to update BuildConfiguration entity
 *
 * @author Jakub Bartecek
 */
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Getter
@Setter
public class BuildOverrides {

    private String scmRevision;

    private String buildScript;

    private Map<String, String> genericParameters = new HashMap<>();

    public BuildConfiguration overrideBuildConfigurationValues(BuildConfiguration buildConfiguration) {
        buildConfiguration.setScmRevision(this.scmRevision);
        buildConfiguration.setBuildScript(this.buildScript);
        buildConfiguration.setGenericParameters(this.genericParameters);
        return buildConfiguration;
    }
}
