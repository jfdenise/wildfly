package org.wildfly.feature.pack.layer.tests.microprofile.openapi;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

@OpenAPIDefinition(info = @Info(title = "hi", version = "1"))
public class OpenApiAnnotationFromRootPackageUsage {
}
