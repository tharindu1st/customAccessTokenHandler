package com.wso2.carbon.apimgt.custom.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.AbstractHandler;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tharindu on 8/2/15.
 */
public class CustomAccessTokenHandler extends AbstractHandler implements ManagedLifecycle {
	private static final Log log = LogFactory.getLog(CustomAccessTokenHandler.class);

	private String authorizationQueryParamName = "access-token";

	@Override
	public boolean handleRequest(MessageContext messageContext) {
		org.apache.axis2.context.MessageContext axis2MC =
				((Axis2MessageContext) messageContext).getAxis2MessageContext();
		String accessToken = "";
		try {
			accessToken = new SynapseXPath("$url:" + authorizationQueryParamName).stringValueOf(messageContext);
		} catch (JaxenException e) {
			log.error("couldn't found token in query parameters", e);
		}

		//remove the query parameter
		if(accessToken != null && !"".equals(accessToken)) {
			String rest_url_postfix = (String) axis2MC.getProperty(NhttpConstants.REST_URL_POSTFIX);
			rest_url_postfix = removeTokenFromQueryParameters(rest_url_postfix, accessToken);
			axis2MC.setProperty(NhttpConstants.REST_URL_POSTFIX, rest_url_postfix);

			//set authorization bearer header to use for the api authentication

			Map headers =
					(Map) axis2MC.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
			if (headers != null) {
				headers.put("Authorization", "Bearer " + accessToken);

			} else {
				headers = new HashMap<String, String>();
				headers.put("Authorization", "Bearer " + accessToken);
				axis2MC.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headers);
			}
		}
		return true;
	}

	@Override
	public boolean handleResponse(MessageContext messageContext) {
		return true;
	}

	@Override
	public void init(SynapseEnvironment synapseEnvironment) {
	}

	@Override
	public void destroy() {

	}

	private String removeTokenFromQueryParameters(String input, String accessToken) {
		input = input.replace("?access-token=" + accessToken, "?");
		input = input.replace("&access-token=" + accessToken, "");
		input = input.replace("?&", "?");
		if (input.lastIndexOf("?") == (input.length() - 1)) {
			input = input.replace("?", "");
		}
		return input;
	}

	public String getAuthorizationQueryParamName() {
		return authorizationQueryParamName;
	}

	public void setAuthorizationQueryParamName(String authorizationQueryParamName) {
		this.authorizationQueryParamName = authorizationQueryParamName;
	}
}
