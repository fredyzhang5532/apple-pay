/*
 * Copyright 2016-2102 Appleframework(http://www.appleframework.com) Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appleframework.pay.trade.utils.alipay.config;

import com.appleframework.config.core.PropertyConfigurer;

/**
 *类名：AlipayConfigUtil
 *功能：基础配置类
 *详细：设置帐户有关信息及返回路径
 *版本：3.4
 *修改日期：2016-03-08
 *说明：
 *以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己网站的需要，按照技术文档编写,并非一定要使用该代码。
 *该代码仅供学习和研究支付宝接口使用，只是提供一个参考。
 */
public class AlipayConfigUtil {
	
	// 应用ID，签约账号，以20开头由16位纯数字组成的字符串，查看地址：https://openhome.alipay.com/platform/appManage.htm
	public static final String app_id = PropertyConfigurer.getString("alipay.app_id");


	// 合作身份者ID，签约账号，以2088开头由16位纯数字组成的字符串，查看地址：https://b.alipay.com/order/pidAndKey.htm
	public static final String partner = PropertyConfigurer.getString("alipay.partner");
	
	// 收款支付宝账号，以2088开头由16位纯数字组成的字符串，一般情况下收款账号就是签约账号
	public static final String seller_id = PropertyConfigurer.getString("alipay.seller_id");

	// MD5密钥，安全检验码，由数字和字母组成的32位字符串，查看地址：https://b.alipay.com/order/pidAndKey.htm
    public static final String key = PropertyConfigurer.getString("alipay.key");

	// 服务器异步通知页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
	public static final String notify_url = PropertyConfigurer.getString("alipay.notify_url");
	
	public static String notify_sign = PropertyConfigurer.getString("alipay.notify_sign");

	// 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
	public static final String return_url = PropertyConfigurer.getString("alipay.return_url");

	// 签名方式
	public static final String sign_type = PropertyConfigurer.getString("alipay.sign_type");
	
	// 调试用，创建TXT日志文件夹路径，见AlipayCore.java类中的logResult(String sWord)打印方法。
	public static final String log_path = PropertyConfigurer.getString("alipay.log_path");
		
	// 字符编码格式 目前支持 gbk 或 utf-8
	public static final String input_charset = PropertyConfigurer.getString("alipay.input_charset", " utf-8");
		
	// 支付类型 ，无需修改
	public static final String payment_type = PropertyConfigurer.getString("alipay.payment_type");
		
	// 调用的接口名，无需修改
	public static final String service = PropertyConfigurer.getString("alipay.service");
	
	// 调用的接口名，无需修改
	public static final String rsa_private_key = PropertyConfigurer.getString("alipay.rsa_private_key");

	
	public static final String rsa_public_key = PropertyConfigurer.getString("alipay.rsa_public_key");

	//↑↑↑↑↑↑↑↑↑↑请在这里配置您的基本信息↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
	
	//↓↓↓↓↓↓↓↓↓↓ 请在这里配置防钓鱼信息，如果没开通防钓鱼功能，为空即可 ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
	
	// 防钓鱼时间戳  若要使用请调用类文件submit中的query_timestamp函数
	public static final String anti_phishing_key = "";
	
	// 客户端的IP地址 非局域网的外网IP地址，如：221.0.0.1
	public static final String exter_invoke_ip = "";
		
	//↑↑↑↑↑↑↑↑↑↑请在这里配置防钓鱼信息，如果没开通防钓鱼功能，为空即可 ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
	
}

