package com.bor.eboard.dms.masterdata;

import com.bor.eboard.common.exception.BusinessException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.dms.dto.DmsMasterDataOptionResponse;
import com.bor.eboard.dms.entity.DmsMasterSource;
import com.bor.eboard.dms.entity.DmsMasterSourceParameter;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class LdapMasterSourceResolver implements DmsMasterSourceResolver {

    private final DmsMasterConfigurationCodec configurationCodec;
    private final DmsMasterParameterBinder parameterBinder;

    public LdapMasterSourceResolver(
            DmsMasterConfigurationCodec configurationCodec,
            DmsMasterParameterBinder parameterBinder) {
        this.configurationCodec = configurationCodec;
        this.parameterBinder = parameterBinder;
    }

    @Override
    public Set<DmsMasterSourceType> supportedTypes() {
        return Set.of(DmsMasterSourceType.LDAP);
    }

    @Override
    public List<DmsMasterDataOptionResponse> resolve(
            DmsMasterSource source,
            List<DmsMasterSourceParameter> parameterDefinitions,
            Map<String, Object> parameters) {
        Map<String, Object> config = configurationCodec.read(source.getConfigurationJson());
        String providerUrl = required(config, "providerUrl");
        String baseDn = required(config, "baseDn");
        String filter = required(config, "filter");

        List<DmsMasterParameterBinder.BoundParameter> bound =
                parameterBinder.bind(parameterDefinitions, parameters);
        for (DmsMasterParameterBinder.BoundParameter parameter : bound) {
            if (parameter.location() == DmsMasterParameterLocation.LDAP_FILTER) {
                filter = filter.replace(
                        "{" + parameter.targetName() + "}",
                        escapeFilter(String.valueOf(parameter.value())));
            }
        }

        Hashtable<String, String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, configurationCodec.resolvePlaceholders(providerUrl));
        Object bindDn = config.get("bindDn");
        Object bindPassword = config.get("bindPassword");
        if (bindDn != null && !String.valueOf(bindDn).isBlank()) {
            if (bindPassword == null || String.valueOf(bindPassword).isBlank()) {
                throw new ValidationException("LDAP bindPassword is required when bindDn is configured");
            }
            environment.put(Context.SECURITY_AUTHENTICATION, "simple");
            environment.put(Context.SECURITY_PRINCIPAL,
                    configurationCodec.resolvePlaceholders(String.valueOf(bindDn)));
            environment.put(Context.SECURITY_CREDENTIALS,
                    configurationCodec.resolvePlaceholders(String.valueOf(bindPassword)));
        }

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setCountLimit(source.getMaxResults() == null ? 500 : source.getMaxResults());
        controls.setReturningAttributes(new String[]{source.getValueField(), source.getLabelField()});

        List<DmsMasterDataOptionResponse> result = new ArrayList<>();
        DirContext context = null;
        NamingEnumeration<SearchResult> matches = null;
        try {
            context = new InitialDirContext(environment);
            matches = context.search(baseDn, filter, controls);
            while (matches.hasMore() && result.size() < controls.getCountLimit()) {
                Attributes attributes = matches.next().getAttributes();
                String value = attributeValue(attributes, source.getValueField());
                String label = attributeValue(attributes, source.getLabelField());
                Map<String, Object> values = new LinkedHashMap<>();
                values.put(source.getValueField(), value);
                values.put(source.getLabelField(), label);
                result.add(new DmsMasterDataOptionResponse(value, label, values));
            }
            return result;
        } catch (NamingException ex) {
            throw new BusinessException("Unable to resolve LDAP master source: " + ex.getMessage());
        } finally {
            close(matches);
            close(context);
        }
    }

    private String required(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new ValidationException("LDAP configuration is missing: " + key);
        }
        return String.valueOf(value);
    }

    private String attributeValue(Attributes attributes, String name) throws NamingException {
        Attribute attribute = attributes.get(name);
        if (attribute == null || attribute.get() == null) {
            throw new BusinessException("LDAP result does not contain attribute: " + name);
        }
        return String.valueOf(attribute.get());
    }

    private String escapeFilter(String value) {
        return value.replace("\\", "\\5c")
                .replace("*", "\\2a")
                .replace("(", "\\28")
                .replace(")", "\\29")
                .replace("\u0000", "\\00");
    }

    private void close(NamingEnumeration<?> enumeration) {
        if (enumeration != null) {
            try {
                enumeration.close();
            } catch (NamingException ignored) {
                // Nothing useful can be done during cleanup.
            }
        }
    }

    private void close(DirContext context) {
        if (context != null) {
            try {
                context.close();
            } catch (NamingException ignored) {
                // Nothing useful can be done during cleanup.
            }
        }
    }
}
