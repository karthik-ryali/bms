package com.banking.virtualwallet.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the statically exported Next.js dashboard from {@code classpath:/static/}
 * so the whole application — modern UI plus REST API — runs on a single port.
 *
 * <p>The export is produced with {@code trailingSlash}, so every route is a
 * directory holding an {@code index.html} (for example {@code /dashboard/} maps
 * to {@code static/dashboard/index.html}). Spring MVC does not resolve a
 * directory to its index file by default, so this resolver adds that fallback
 * and, for unknown extension-less paths, hands back the root SPA shell so
 * client-side routing can take over.</p>
 *
 * <p>REST controllers are matched before resource handlers, so
 * {@code /api/**} is never affected.</p>
 */
@Configuration
public class StaticUiConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // Never swallow API traffic — controllers (and the error
                        // handler) must be the ones to answer /api/**.
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }

                        if (resourcePath.isEmpty()) {
                            return index(location, "");
                        }

                        Resource direct = location.createRelative(resourcePath);
                        if (direct.exists() && direct.isReadable()) {
                            return direct;
                        }

                        // Directory route -> <route>/index.html (trailingSlash export).
                        Resource indexed = index(location, resourcePath);
                        if (indexed != null) {
                            return indexed;
                        }

                        // Unknown, extension-less path -> the app shell, so
                        // deep links resolve client-side instead of 404ing.
                        if (!resourcePath.contains(".")) {
                            return index(location, "");
                        }

                        return null;
                    }

                    private Resource index(Resource location, String resourcePath) throws IOException {
                        String base = resourcePath.isEmpty() || resourcePath.endsWith("/")
                                ? resourcePath
                                : resourcePath + "/";
                        Resource candidate = location.createRelative(base + "index.html");
                        return candidate.exists() && candidate.isReadable() ? candidate : null;
                    }
                });
    }
}
