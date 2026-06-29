package com.king_sparkon_tracker.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "google.pay")
public class GooglePayProperties {

	private String environment = "TEST";
	private String merchantId;
	private String merchantName = "King Sparkon Tracker";
	private String gateway = "stripe";
	private String gatewayMerchantId;

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getMerchantId() {
		return merchantId;
	}

	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}

	public String getMerchantName() {
		return merchantName;
	}

	public void setMerchantName(String merchantName) {
		this.merchantName = merchantName;
	}

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	public String getGatewayMerchantId() {
		return gatewayMerchantId;
	}

	public void setGatewayMerchantId(String gatewayMerchantId) {
		this.gatewayMerchantId = gatewayMerchantId;
	}
}
