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
package com.appleframework.pay.trade.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.ExtendParams;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.appleframework.config.core.PropertyConfigurer;
import com.appleframework.pay.account.service.RpAccountTransactionService;
import com.appleframework.pay.common.core.enums.PayTypeEnum;
import com.appleframework.pay.common.core.enums.PayWayEnum;
import com.appleframework.pay.common.core.utils.DateUtils;
import com.appleframework.pay.common.core.utils.StringUtil;
import com.appleframework.pay.notify.service.RpNotifyService;
import com.appleframework.pay.trade.dao.RpTradePaymentOrderDao;
import com.appleframework.pay.trade.dao.RpTradePaymentRecordDao;
import com.appleframework.pay.trade.entity.GoodsDetails;
import com.appleframework.pay.trade.entity.RpTradePaymentOrder;
import com.appleframework.pay.trade.entity.RpTradePaymentRecord;
import com.appleframework.pay.trade.entity.RpUserPayOauth;
import com.appleframework.pay.trade.entity.weixinpay.WeiXinPrePay;
import com.appleframework.pay.trade.enums.OrderFromEnum;
import com.appleframework.pay.trade.enums.TradeStatusEnum;
import com.appleframework.pay.trade.enums.TrxTypeEnum;
import com.appleframework.pay.trade.enums.alipay.AliPayTradeStateEnum;
import com.appleframework.pay.trade.enums.weixinpay.WeiXinTradeTypeEnum;
import com.appleframework.pay.trade.enums.weixinpay.WeixinTradeStateEnum;
import com.appleframework.pay.trade.exception.TradeBizException;
import com.appleframework.pay.trade.model.OrderPayBo;
import com.appleframework.pay.trade.service.RpTradePaymentManagerService;
import com.appleframework.pay.trade.service.RpUserPayOauthService;
import com.appleframework.pay.trade.utils.MerchantApiUtil;
import com.appleframework.pay.trade.utils.WeiXinPayUtils;
import com.appleframework.pay.trade.utils.alipay.config.AlipayConfigUtil;
import com.appleframework.pay.trade.utils.alipay.f2fpay.AliF2FPaySubmit;
import com.appleframework.pay.trade.utils.alipay.util.AlipayNotify;
import com.appleframework.pay.trade.utils.alipay.util.AlipaySubmit;
import com.appleframework.pay.trade.utils.alipay.util.ApplePayNotify;
import com.appleframework.pay.trade.utils.alipay.util.ApplePayUtil;
import com.appleframework.pay.trade.utils.apple.AppleReceiptBean;
import com.appleframework.pay.trade.utils.apple.AppleVerifyBean;
import com.appleframework.pay.trade.vo.AppPayResultVo;
import com.appleframework.pay.trade.vo.F2FPayResultVo;
import com.appleframework.pay.trade.vo.OrderPayResultVo;
import com.appleframework.pay.trade.vo.RpPayGateWayPageShowVo;
import com.appleframework.pay.trade.vo.ScanPayResultVo;
import com.appleframework.pay.user.entity.RpPayWay;
import com.appleframework.pay.user.entity.RpUserInfo;
import com.appleframework.pay.user.entity.RpUserPayConfig;
import com.appleframework.pay.user.entity.RpUserPayInfo;
import com.appleframework.pay.user.enums.FundInfoTypeEnum;
import com.appleframework.pay.user.exception.UserBizException;
import com.appleframework.pay.user.service.BuildNoService;
import com.appleframework.pay.user.service.RpPayWayService;
import com.appleframework.pay.user.service.RpUserInfoService;
import com.appleframework.pay.user.service.RpUserPayConfigService;
import com.appleframework.pay.user.service.RpUserPayInfoService;
import com.appleframework.pay.utils.UrlMapUtility;
import com.egzosn.pay.common.api.PayService;
import com.egzosn.pay.common.bean.PayOrder;
import com.egzosn.pay.wx.api.WxPayConfigStorage;
import com.egzosn.pay.wx.api.WxPayService;
import com.egzosn.pay.wx.bean.WxTransactionType;
import com.taobao.diamond.utils.JSONUtils;

/**
 * <b>功能说明:交易模块管理实现类实现</b>
 * @author  Cruise.Xu
 * <a href="http://www.appleframework.com">appleframework(http://www.appleframework.com)</a>
 */
@Service("rpTradePaymentManagerService")
public class RpTradePaymentManagerServiceImpl implements RpTradePaymentManagerService {

    private static final Logger LOG = LoggerFactory.getLogger(RpTradePaymentManagerServiceImpl.class);

    @Autowired
    private RpTradePaymentOrderDao rpTradePaymentOrderDao;

    @Autowired
    private RpTradePaymentRecordDao rpTradePaymentRecordDao;

    @Autowired
    private RpUserInfoService rpUserInfoService;

    @Autowired
    private RpUserPayInfoService rpUserPayInfoService;

    @Autowired
    private RpUserPayConfigService rpUserPayConfigService;

    @Autowired
    private RpPayWayService rpPayWayService;

    @Autowired
    private BuildNoService buildNoService;

    @Autowired
    private RpNotifyService rpNotifyService;

    @Autowired
    private RpAccountTransactionService rpAccountTransactionService;

    @Autowired
    private AliF2FPaySubmit aliF2FPaySubmit;
    
	@Autowired
	private RpUserPayOauthService rpUserPayOauthService;

    /**
     * 初始化直连扫码支付数据,直连扫码支付初始化方法规则
     * 1:根据(商户编号 + 商户订单号)确定订单是否存在
     * 1.1:如果订单存在,抛异常,提示订单已存在
     * 1.2:如果订单不存在,创建支付订单
     * 2:创建支付记录
     * 3:根据相应渠道方法
     * 4:调转到相应支付渠道扫码界面
     *
     * @param payKey  商户支付KEY
     * @param productName 产品名称
     * @param orderNo     商户订单号
     * @param orderDate   下单日期
     * @param orderTime   下单时间
     * @param orderPrice  订单金额(元)
     * @param payWayCode      支付方式编码
     * @param orderIp     下单IP
     * @param orderPeriod 订单有效期(分钟)
     * @param returnUrl   支付结果页面通知地址
     * @param notifyUrl   支付结果后台通知地址
     * @param remark      支付备注
     * @param field1      扩展字段1
     * @param field2      扩展字段2
     * @param field3      扩展字段3
     * @param field4      扩展字段4
     * @param field5      扩展字段5
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScanPayResultVo initDirectScanPay(String payKey, String payWayCode, OrderPayBo bo) {

        RpUserPayConfig rpUserPayConfig = rpUserPayConfigService.getByPayKey(payKey);
        if (rpUserPayConfig == null){
            throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR,"用户支付配置有误");
        }

        //根据支付产品及支付方式获取费率
        RpPayWay payWay = null;
        PayTypeEnum payType = null;
        if (PayWayEnum.WEIXIN.name().equals(payWayCode)){
            payWay = rpPayWayService.getByPayWayTypeCode(rpUserPayConfig.getProductCode(), payWayCode, PayTypeEnum.SCANPAY.name());
            payType = PayTypeEnum.SCANPAY;
        }else if (PayWayEnum.ALIPAY.name().equals(payWayCode)){
            payWay = rpPayWayService.getByPayWayTypeCode(rpUserPayConfig.getProductCode(), payWayCode, PayTypeEnum.DIRECT_PAY.name());
            payType = PayTypeEnum.DIRECT_PAY;
        }

        if(payWay == null){
            throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR,"用户支付配置有误");
        }

        String merchantNo = rpUserPayConfig.getUserNo();//商户编号

        RpUserInfo rpUserInfo = rpUserInfoService.getDataByMerchentNo(merchantNo);
        if (rpUserInfo == null){
            throw new UserBizException(UserBizException.USER_IS_NULL,"用户不存在");
        }

        RpTradePaymentOrder rpTradePaymentOrder = rpTradePaymentOrderDao.selectByMerchantNoAndMerchantOrderNo(merchantNo, bo.getOrderNo());
        if (rpTradePaymentOrder == null){
            rpTradePaymentOrder = sealRpTradePaymentOrder(merchantNo, rpUserInfo.getUserName(), payWayCode, PayWayEnum.getEnum(payWayCode).getDesc(), 
            		payType, rpUserPayConfig.getFundIntoType(),  bo);
            rpTradePaymentOrderDao.insert(rpTradePaymentOrder);
        }else{

            if (TradeStatusEnum.SUCCESS.name().equals(rpTradePaymentOrder.getStatus())){
                throw new TradeBizException(TradeBizException.TRADE_ORDER_ERROR,"订单已支付成功,无需重复支付");
            }

            if (rpTradePaymentOrder.getOrderAmount().compareTo(bo.getOrderPrice()) != 0 ){
                rpTradePaymentOrder.setOrderAmount(bo.getOrderPrice());//如果金额不一致,修改金额为最新的金额
            }
        }

        return getScanPayResultVo(rpTradePaymentOrder , payWay);

    }
    
    /**
     * 初始化直连扫码支付数据,直连扫码支付初始化方法规则
     * 1:根据(商户编号 + 商户订单号)确定订单是否存在
     * 1.1:如果订单存在,抛异常,提示订单已存在
     * 1.2:如果订单不存在,创建支付订单
     * 2:创建支付记录
     * 3:根据相应渠道方法
     * 4:调转到相应支付渠道扫码界面
     *
     * @param payKey  商户支付KEY
     * @param productName 产品名称
     * @param orderNo     商户订单号
     * @param orderDate   下单日期
     * @param orderTime   下单时间
     * @param orderPrice  订单金额(元)
     * @param payWayCode      支付方式编码
     * @param orderIp     下单IP
     * @param orderPeriod 订单有效期(分钟)
     * @param returnUrl   支付结果页面通知地址
     * @param notifyUrl   支付结果后台通知地址
     * @param remark      支付备注
     * @param field1      扩展字段1
     * @param field2      扩展字段2
     * @param field3      扩展字段3
     * @param field4      扩展字段4
     * @param field5      扩展字段5
     */
    @Override
	@Transactional(rollbackFor = Exception.class)
	public AppPayResultVo initDirectAppPay(String payKey, String payWayCode, OrderPayBo bo) {

        RpUserPayConfig rpUserPayConfig = rpUserPayConfigService.getByPayKey(payKey);
        if (rpUserPayConfig == null){
        	LOG.error("用户支付配置有误:" + payKey);
            throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR,"用户支付配置有误");
        }
        LOG.info("用户支付配置:" + JSON.toJSONString(rpUserPayConfig));

        //根据支付产品及支付方式获取费率
        RpPayWay payWay = null;
        PayTypeEnum payType = null;
		if (PayWayEnum.WEIXIN.name().equals(payWayCode)) {
			payWay = rpPayWayService.getByPayWayTypeCode(rpUserPayConfig.getProductCode(), payWayCode, PayTypeEnum.APPPAY.name());
			payType = PayTypeEnum.APPPAY;
		} else if (PayWayEnum.ALIPAY.name().equals(payWayCode)) {
			payWay = rpPayWayService.getByPayWayTypeCode(rpUserPayConfig.getProductCode(), payWayCode, PayTypeEnum.DIRECT_PAY.name());
			payType = PayTypeEnum.DIRECT_PAY;
		}  else if (PayWayEnum.APPLE.name().equals(payWayCode)) {
			payWay = rpPayWayService.getByPayWayTypeCode(rpUserPayConfig.getProductCode(), payWayCode, PayTypeEnum.IN_APP.name());
			payType = PayTypeEnum.IN_APP;
		}

		if (payWay == null) {
			LOG.error("用户支付配置有误:" + payKey);
			throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR, "用户支付配置有误");
		}

		LOG.info("用户支付方式配置:" + JSON.toJSONString(payWay));

		String merchantNo = rpUserPayConfig.getUserNo();// 商户编号

		LOG.info("商户编号:" + merchantNo);

		RpUserInfo rpUserInfo = rpUserInfoService.getDataByMerchentNo(merchantNo);
		if (rpUserInfo == null) {
			LOG.error("商户不存在:" + merchantNo);
			throw new UserBizException(UserBizException.USER_IS_NULL, "商户不存在");
		}
		
		LOG.info("商户信息:" + JSON.toJSONString(rpUserInfo));

        RpTradePaymentOrder rpTradePaymentOrder = rpTradePaymentOrderDao.selectByMerchantNoAndMerchantOrderNo(merchantNo, bo.getOrderNo());
        if (rpTradePaymentOrder == null){
            rpTradePaymentOrder = sealRpTradePaymentOrder( merchantNo,  rpUserInfo.getUserName(), payWayCode, PayWayEnum.getEnum(payWayCode).getDesc() , 
            		payType, rpUserPayConfig.getFundIntoType(),  bo);
            rpTradePaymentOrderDao.insert(rpTradePaymentOrder);
        } else {
            if (TradeStatusEnum.SUCCESS.name().equals(rpTradePaymentOrder.getStatus())){
                throw new TradeBizException(TradeBizException.TRADE_ORDER_ERROR,"订单已支付成功,无需重复支付");
            }
            if (rpTradePaymentOrder.getOrderAmount().compareTo(bo.getOrderPrice()) != 0 ){
                rpTradePaymentOrder.setOrderAmount(bo.getOrderPrice());//如果金额不一致,修改金额为最新的金额
            }
        }

        return getAppPayResultVo(rpTradePaymentOrder , payWay, payType);

    }
    
    /**
     * 初始化直连扫码支付数据,直连扫码支付初始化方法规则
     * 1:根据(商户编号 + 商户订单号)确定订单是否存在
     * 1.1:如果订单存在,抛异常,提示订单已存在
     * 1.2:如果订单不存在,创建支付订单
     * 2:创建支付记录
     * 3:根据相应渠道方法
     * 4:调转到相应支付渠道扫码界面
     *
     * @param payKey  商户支付KEY
     * @param productName 产品名称
     * @param orderNo     商户订单号
     * @param orderDate   下单日期
     * @param orderTime   下单时间
     * @param orderPrice  订单金额(元)
     * @param payWayCode      支付方式编码
     * @param orderIp     下单IP
     * @param orderPeriod 订单有效期(分钟)
     * @param returnUrl   支付结果页面通知地址
     * @param notifyUrl   支付结果后台通知地址
     * @param remark      支付备注
     * @param field1      扩展字段1
     * @param field2      扩展字段2
     * @param field3      扩展字段3
     * @param field4      扩展字段4
     * @param field5      扩展字段5
     */
    @Override
	@Transactional(rollbackFor = Exception.class)
	public AppPayResultVo initDirectJsapiPay(String payKey, String payWayCode, OrderPayBo bo) {

        RpUserPayConfig rpUserPayConfig = rpUserPayConfigService.getByPayKey(payKey);
        if (rpUserPayConfig == null){
        	LOG.error("用户支付配置有误:" + payKey);
            throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR,"用户支付配置有误");
        }
        LOG.info("用户支付配置:" + JSON.toJSONString(rpUserPayConfig));

        //根据支付产品及支付方式获取费率
        RpPayWay payWay = null;
        PayTypeEnum payType = null;
		if (PayWayEnum.WEIXIN.name().equals(payWayCode)) {
			payWay = rpPayWayService.getByPayWayTypeCode(rpUserPayConfig.getProductCode(), payWayCode, PayTypeEnum.WX_PROGRAM_PAY.name());
			payType = PayTypeEnum.WX_PROGRAM_PAY;
		} else if (PayWayEnum.ALIPAY.name().equals(payWayCode)) {
			payWay = rpPayWayService.getByPayWayTypeCode(rpUserPayConfig.getProductCode(), payWayCode, PayTypeEnum.DIRECT_PAY.name());
			payType = PayTypeEnum.DIRECT_PAY;
		}  else {
			LOG.error("用户支付配置有误:" + payWayCode);
		}

		if (payWay == null) {
			LOG.error("用户支付配置有误:" + payKey);
			throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR, "用户支付配置有误");
		}

		LOG.info("用户支付方式配置:" + JSON.toJSONString(payWay));

		String merchantNo = rpUserPayConfig.getUserNo();// 商户编号

		LOG.info("商户编号:" + merchantNo);

		RpUserInfo rpUserInfo = rpUserInfoService.getDataByMerchentNo(merchantNo);
		if (rpUserInfo == null) {
			LOG.error("商户不存在:" + merchantNo);
			throw new UserBizException(UserBizException.USER_IS_NULL, "商户不存在");
		}
		
		LOG.info("商户信息:" + JSON.toJSONString(rpUserInfo));

        RpTradePaymentOrder rpTradePaymentOrder = rpTradePaymentOrderDao.selectByMerchantNoAndMerchantOrderNo(merchantNo, bo.getOrderNo());
        if (rpTradePaymentOrder == null){
            rpTradePaymentOrder = sealRpTradePaymentOrder(
            		merchantNo,  rpUserInfo.getUserName(),  
            		payWayCode, PayWayEnum.getEnum(payWayCode).getDesc() , payType, rpUserPayConfig.getFundIntoType(), bo);
            rpTradePaymentOrderDao.insert(rpTradePaymentOrder);
        } else {
            if (TradeStatusEnum.SUCCESS.name().equals(rpTradePaymentOrder.getStatus())){
                throw new TradeBizException(TradeBizException.TRADE_ORDER_ERROR,"订单已支付成功,无需重复支付");
            }
            if (rpTradePaymentOrder.getOrderAmount().compareTo(bo.getOrderPrice()) != 0 ){
                rpTradePaymentOrder.setOrderAmount(bo.getOrderPrice());//如果金额不一致,修改金额为最新的金额
            }
        }

        return getAppPayResultVo(rpTradePaymentOrder , payWay, payType);

    }

    /**
     * 条码支付,对应的是支付宝的条码支付或者微信的刷卡支付
     *
     * @param payKey      商户支付key
     * @param authCode    支付授权码
     * @param productName 产品名称
     * @param orderNo     商户订单号
     * @param orderDate   下单日期
     * @param orderTime   下单时间
     * @param orderPrice  订单金额(元)
     * @param payWayCode  支付方式
     * @param orderIp     下单IP
     * @param remark      支付备注
     * @param field1      扩展字段1
     * @param field2      扩展字段2
     * @param field3      扩展字段3
     * @param field4      扩展字段4
     * @param field5      扩展字段5
     * @return
     */
    @Override
    public F2FPayResultVo f2fPay(String payKey, String authCode, String payWayCode, OrderPayBo bo) {

        RpUserPayConfig rpUserPayConfig = rpUserPayConfigService.getByPayKey(payKey);
        if (rpUserPayConfig == null){
            throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR,"用户支付配置有误");
        }

        if (StringUtil.isEmpty(authCode)){
            throw new TradeBizException(TradeBizException.TRADE_PARAM_ERROR,"支付授权码不能为空");
        }
        //根据支付产品及支付方式获取费率
        RpPayWay payWay = null;
        PayTypeEnum payType = null;
        if (PayWayEnum.WEIXIN.name().equals(payWayCode)){
//            payWay = rpPayWayService.getByPayWayTypeCode(rpUserPayConfig.getProductCode(), payWayCode, PayTypeEnum.SCANPAY.name());
            payType = PayTypeEnum.SCANPAY;//TODO 具体需要根据接口修改
        }else if (PayWayEnum.ALIPAY.name().equals(payWayCode)){
            payWay = rpPayWayService.getByPayWayTypeCode(rpUserPayConfig.getProductCode(), payWayCode, PayTypeEnum.F2F_PAY.name());
            payType = PayTypeEnum.F2F_PAY;
        }

        if(payWay == null){
            throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR,"用户支付配置有误");
        }

        String merchantNo = rpUserPayConfig.getUserNo();//商户编号
        RpUserInfo rpUserInfo = rpUserInfoService.getDataByMerchentNo(merchantNo);
        if (rpUserInfo == null){
            throw new UserBizException(UserBizException.USER_IS_NULL,"用户不存在");
        }

        RpTradePaymentOrder rpTradePaymentOrder = rpTradePaymentOrderDao.selectByMerchantNoAndMerchantOrderNo(merchantNo, bo.getOrderNo());
        if (rpTradePaymentOrder == null){
            bo.setReturnUrl("f2fPay");
            bo.setNotifyUrl("f2fPay");
            
            rpTradePaymentOrder = sealRpTradePaymentOrder(
            		merchantNo,  rpUserInfo.getUserName(), payWayCode, 
            		PayWayEnum.getEnum(payWayCode).getDesc(), payType, rpUserPayConfig.getFundIntoType(), bo);
            rpTradePaymentOrderDao.insert(rpTradePaymentOrder);
        }else{
            if (rpTradePaymentOrder.getOrderAmount().compareTo(bo.getOrderPrice()) != 0 ){
                throw new TradeBizException(TradeBizException.TRADE_ORDER_ERROR,"错误的订单");
            }

            if (TradeStatusEnum.SUCCESS.name().equals(rpTradePaymentOrder.getStatus())){
                throw new TradeBizException(TradeBizException.TRADE_ORDER_ERROR,"订单已支付成功,无需重复支付");
            }
        }

        return getF2FPayResultVo(rpTradePaymentOrder , payWay ,  payKey , rpUserPayConfig.getPaySecret() , authCode ,null);
    }

    /**
     * 通过支付订单及商户费率生成支付记录
     * @param rpTradePaymentOrder   支付订单
     * @param payWay   商户支付配置
     * @return
     */
    private F2FPayResultVo getF2FPayResultVo(RpTradePaymentOrder rpTradePaymentOrder ,RpPayWay payWay , 
    		String payKey, String merchantPaySecret, String authCode, List< GoodsDetails > goodsDetailses){

        F2FPayResultVo f2FPayResultVo = new F2FPayResultVo();
        String payWayCode = payWay.getPayWayCode();//支付方式

        PayTypeEnum payType = null;
        if (PayWayEnum.WEIXIN.name().equals(payWay.getPayWayCode())){
            payType = PayTypeEnum.SCANPAY;//TODO 微信条码支付需要修改成对应的枚举 支付类型
        }else if(PayWayEnum.ALIPAY.name().equals(payWay.getPayWayCode())){
            payType = PayTypeEnum.F2F_PAY;
        }

        rpTradePaymentOrder.setPayTypeCode(payType.name());//支付类型
        rpTradePaymentOrder.setPayTypeName(payType.getDesc());//支付方式

        rpTradePaymentOrder.setPayWayCode(payWay.getPayWayCode());
        rpTradePaymentOrder.setPayWayName(payWay.getPayWayName());
        
        OrderPayBo bo = this.changToOrderByBo(rpTradePaymentOrder);

        RpTradePaymentRecord rpTradePaymentRecord = sealRpTradePaymentRecord(
        		rpTradePaymentOrder.getMerchantNo(),  rpTradePaymentOrder.getMerchantName() , 
        		payWay.getPayWayCode(),  payWay.getPayWayName() , payType, 
        		rpTradePaymentOrder.getFundIntoType()  , BigDecimal.valueOf(payWay.getPayRate()),  bo);
        rpTradePaymentRecordDao.insert(rpTradePaymentRecord);

        if (PayWayEnum.WEIXIN.name().equals(payWayCode)){//微信支付
            throw new TradeBizException(TradeBizException.TRADE_PAY_WAY_ERROR,"暂未开通微信刷卡支付");
        }else {
            if (PayWayEnum.ALIPAY.name().equals(payWayCode)) {//支付宝支付

                RpUserPayInfo rpUserPayInfo = rpUserPayInfoService.getByUserNo(rpTradePaymentOrder.getMerchantNo(),payWayCode);
                if (rpUserPayInfo == null){
                    throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR,"商户支付配置有误");
                }

                aliF2FPaySubmit.initConfigs(rpTradePaymentOrder.getFundIntoType(), rpUserPayInfo.getOfflineAppId(), rpUserPayInfo.getAppId(), rpUserPayInfo.getRsaPrivateKey(), rpUserPayInfo.getRsaPublicKey());
                Map<String , String > aliPayReturnMsg = aliF2FPaySubmit.f2fPay(rpTradePaymentRecord.getBankOrderNo(), rpTradePaymentOrder.getProductName(), "", authCode, rpTradePaymentRecord.getOrderAmount(), goodsDetailses);

                if(TradeStatusEnum.SUCCESS.name().equals(aliPayReturnMsg.get("status"))){//支付成功
                    completeSuccessOrder( rpTradePaymentRecord ,  aliPayReturnMsg.get("bankTrxNo") ,new Date() ,  aliPayReturnMsg.get("bankReturnMsg"));
                }else if(TradeStatusEnum.FAILED.name().equals(aliPayReturnMsg.get("status"))){//支付失败
                    completeFailOrder(rpTradePaymentRecord , aliPayReturnMsg.get("bankReturnMsg"));
                }else{
                    //TODO 未知支付结果,需要在后续添加订单结果轮询功能后处理
                }

            } else {
                throw new TradeBizException(TradeBizException.TRADE_PAY_WAY_ERROR, "错误的支付方式");
            }
        }

        Map<String , Object> paramMap = new HashMap<String , Object>();
        f2FPayResultVo.setStatus(rpTradePaymentRecord.getStatus());//支付结果
        paramMap.put("status",rpTradePaymentRecord.getStatus());

        f2FPayResultVo.setField1(rpTradePaymentRecord.getField1());//扩展字段1
        paramMap.put("field1",rpTradePaymentRecord.getField1());

        f2FPayResultVo.setField2(rpTradePaymentRecord.getField2());//扩展字段2
        paramMap.put("field2",rpTradePaymentRecord.getField2());

        f2FPayResultVo.setField3(rpTradePaymentRecord.getField3());//扩展字段3
        paramMap.put("field3",rpTradePaymentRecord.getField3());

        f2FPayResultVo.setField4(rpTradePaymentRecord.getField4());//扩展字段4
        paramMap.put("field4",rpTradePaymentRecord.getField4());

        f2FPayResultVo.setField5(rpTradePaymentRecord.getField5());//扩展字段5
        paramMap.put("field5",rpTradePaymentRecord.getField5());

        f2FPayResultVo.setOrderIp(rpTradePaymentRecord.getOrderIp());//下单ip
        paramMap.put("orderIp",rpTradePaymentRecord.getOrderIp());

        f2FPayResultVo.setOrderNo(rpTradePaymentRecord.getMerchantOrderNo());//商户订单号
        paramMap.put("merchantOrderNo",rpTradePaymentRecord.getMerchantOrderNo());

        f2FPayResultVo.setPayKey(payKey);//支付号
        paramMap.put("payKey",payKey);

        f2FPayResultVo.setProductName(rpTradePaymentRecord.getProductName());//产品名称
        paramMap.put("productName",rpTradePaymentRecord.getProductName());

        f2FPayResultVo.setRemark(rpTradePaymentRecord.getRemark());//支付备注
        paramMap.put("remark",rpTradePaymentRecord.getRemark());

        f2FPayResultVo.setTrxNo(rpTradePaymentRecord.getTrxNo());//交易流水号
        paramMap.put("trxNo", rpTradePaymentRecord.getTrxNo());

        String sign = MerchantApiUtil.getSign(paramMap, merchantPaySecret);

        f2FPayResultVo.setSign(sign);
        return f2FPayResultVo;
    }



    /**
     * 支付成功方法
     * @param rpTradePaymentRecord
     */
	private void completeSuccessOrder(RpTradePaymentRecord rpTradePaymentRecord, String bankTrxNo, Date timeEnd, String bankReturnMsg) {

		LOG.info("completeSuccessOrder:rpTradePaymentRecord:" + rpTradePaymentRecord);
		LOG.info("completeSuccessOrder:bankTrxNo:" + bankTrxNo);
		LOG.info("completeSuccessOrder:timeEnd:" + timeEnd);
		LOG.info("completeSuccessOrder:bankReturnMsg:" + bankReturnMsg);
		rpTradePaymentRecord.setPaySuccessTime(timeEnd);
		rpTradePaymentRecord.setBankTrxNo(bankTrxNo);// 设置银行流水号
		rpTradePaymentRecord.setBankReturnMsg(bankReturnMsg);
		rpTradePaymentRecord.setStatus(TradeStatusEnum.SUCCESS.name());
		rpTradePaymentRecordDao.update(rpTradePaymentRecord);
		
		RpTradePaymentOrder rpTradePaymentOrder = rpTradePaymentOrderDao.selectByMerchantNoAndMerchantOrderNo(rpTradePaymentRecord.getMerchantNo(), rpTradePaymentRecord.getMerchantOrderNo());
		rpTradePaymentOrder.setStatus(TradeStatusEnum.SUCCESS.name());
		rpTradePaymentOrder.setTrxNo(rpTradePaymentRecord.getTrxNo());// 设置支付平台支付流水号
		rpTradePaymentOrderDao.update(rpTradePaymentOrder);
		
		LOG.info("completeSuccessOrder:rpTradePaymentOrder:" + rpTradePaymentOrder);
		LOG.info("completeSuccessOrder:TrxNo:" + rpTradePaymentRecord.getTrxNo());

		//if (FundInfoTypeEnum.PLAT_RECEIVES.name().equals(rpTradePaymentRecord.getFundIntoType())) {
			rpAccountTransactionService.creditToAccount(rpTradePaymentRecord.getMerchantNo(),
					rpTradePaymentRecord.getOrderAmount().subtract(rpTradePaymentRecord.getPlatIncome()),
					rpTradePaymentRecord.getBankOrderNo(), rpTradePaymentRecord.getBankTrxNo(),
					rpTradePaymentRecord.getTrxType(), rpTradePaymentRecord.getRemark());
		//}
		 

		if (PayTypeEnum.F2F_PAY.name().equals(rpTradePaymentOrder.getPayTypeCode())) {
			// 支付宝	条码支付实时返回支付结果,不需要商户通知
			LOG.info("completeSuccessOrder:getPayTypeCode:" + rpTradePaymentOrder.getPayTypeCode());
			return;
		} else {
			LOG.info("completeSuccessOrder------->>>>");
			String notifyUrl = getMerchantNotifyUrl(rpTradePaymentRecord, rpTradePaymentOrder, rpTradePaymentRecord.getNotifyUrl(), TradeStatusEnum.SUCCESS);
			LOG.info("completeSuccessOrder:notifyUrl:" + notifyUrl);
			rpNotifyService.notifySend(notifyUrl, rpTradePaymentRecord.getMerchantOrderNo(), rpTradePaymentRecord.getMerchantNo());
		}
	}


    private String getMerchantNotifyUrl(RpTradePaymentRecord rpTradePaymentRecord ,RpTradePaymentOrder rpTradePaymentOrder ,String sourceUrl , 
    		TradeStatusEnum tradeStatusEnum){
    	LOG.info("completeSuccessOrder------->>>>getMerchantNotifyUrl");
        RpUserPayConfig rpUserPayConfig = rpUserPayConfigService.getByUserNo(rpTradePaymentRecord.getMerchantNo());
        LOG.info("getMerchantNotifyUrl:rpUserPayConfig:" + rpUserPayConfig);
        if (rpUserPayConfig == null){
            throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR,"用户支付配置有误");
        }

        Map<String , Object> paramMap = new HashMap<>();

        String payKey = rpUserPayConfig.getPayKey();// 企业支付KEY
        paramMap.put("payKey",payKey);
        String productName = rpTradePaymentRecord.getProductName(); // 商品名称
        paramMap.put("productName",productName);
        String orderNo = rpTradePaymentRecord.getMerchantOrderNo(); // 订单编号
        paramMap.put("orderNo",orderNo);
        BigDecimal orderPrice = rpTradePaymentRecord.getOrderAmount(); // 订单金额 , 单位:元
        paramMap.put("orderPrice",orderPrice);
        String payWayCode = rpTradePaymentRecord.getPayWayCode(); // 支付方式编码 支付宝: ALIPAY  微信:WEIXIN
        paramMap.put("payWayCode",payWayCode);
        paramMap.put("tradeStatus",tradeStatusEnum);//交易状态
        String orderDateStr = DateUtils.formatDate(rpTradePaymentOrder.getOrderDate(),"yyyyMMdd"); // 订单日期
        paramMap.put("orderDate",orderDateStr);
        String orderTimeStr = DateUtils.formatDate(rpTradePaymentOrder.getOrderTime(), "yyyyMMddHHmmss"); // 订单时间
        paramMap.put("orderTime",orderTimeStr);
        String remark = rpTradePaymentRecord.getRemark(); // 支付备注
        paramMap.put("remark",remark);
        String trxNo = rpTradePaymentRecord.getTrxNo();//支付流水号
        paramMap.put("trxNo",trxNo);

        String field1 = rpTradePaymentOrder.getField1(); // 扩展字段1
        paramMap.put("field1",field1);
        String field2 = rpTradePaymentOrder.getField2(); // 扩展字段2
        paramMap.put("field2",field2);
        String field3 = rpTradePaymentOrder.getField3(); // 扩展字段3
        paramMap.put("field3",field3);
        String field4 = rpTradePaymentOrder.getField4(); // 扩展字段4
        paramMap.put("field4",field4);
        String field5 = rpTradePaymentOrder.getField5(); // 扩展字段5
        paramMap.put("field5",field5);

        LOG.info("getMerchantNotifyUrl:paramMap:" + paramMap);
        
        String paramStr = MerchantApiUtil.getParamStr(paramMap);
        String sign = MerchantApiUtil.getSign(paramMap, rpUserPayConfig.getPaySecret());
        String notifyUrl = sourceUrl + "?" + paramStr + "&sign=" + sign;

        LOG.info("getMerchantNotifyUrl:paramStr:" + paramStr);
        LOG.info("getMerchantNotifyUrl:notifyUrl:" + notifyUrl);
        return notifyUrl;
    }


    /**
     * 支付失败方法
     * @param rpTradePaymentRecord
     */
    private void completeFailOrder(RpTradePaymentRecord rpTradePaymentRecord , String bankReturnMsg){
        rpTradePaymentRecord.setBankReturnMsg(bankReturnMsg);
        rpTradePaymentRecord.setStatus(TradeStatusEnum.FAILED.name());
        rpTradePaymentRecordDao.update(rpTradePaymentRecord);

        RpTradePaymentOrder rpTradePaymentOrder = 
        		rpTradePaymentOrderDao.selectByMerchantNoAndMerchantOrderNo(rpTradePaymentRecord.getMerchantNo(), rpTradePaymentRecord.getMerchantOrderNo());
        rpTradePaymentOrder.setStatus(TradeStatusEnum.FAILED.name());
        rpTradePaymentOrderDao.update(rpTradePaymentOrder);

        String  notifyUrl = getMerchantNotifyUrl(rpTradePaymentRecord , rpTradePaymentOrder , rpTradePaymentRecord.getNotifyUrl() ,TradeStatusEnum.FAILED );
        rpNotifyService.notifySend(notifyUrl, rpTradePaymentRecord.getMerchantOrderNo(), rpTradePaymentRecord.getMerchantNo());
    }

    /**
     * 初始化非直连扫码支付数据,非直连扫码支付初始化方法规则
     * 1:根据(商户编号 + 商户订单号)确定订单是否存在
     * 1.1:如果订单存在且为未支付,抛异常提示订单已存在
     * 1.2:如果订单不存在,创建支付订单
     * 2:获取商户支付配置,跳转到支付网关,选择支付方式
     *
     * @param payKey  商户支付KEY
     * @param productName 产品名称
     * @param orderNo     商户订单号
     * @param orderDate   下单日期
     * @param orderTime   下单时间
     * @param orderPrice  订单金额(元)
     * @param orderIp     下单IP
     * @param orderPeriod 订单有效期(分钟)
     * @param returnUrl   支付结果页面通知地址
     * @param notifyUrl   支付结果后台通知地址
     * @param remark      支付备注
     * @param field1      扩展字段1
     * @param field2      扩展字段2
     * @param field3      扩展字段3
     * @param field4      扩展字段4
     * @param field5      扩展字段5
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RpPayGateWayPageShowVo initNonDirectScanPay(String payKey, OrderPayBo bo) {

        RpUserPayConfig rpUserPayConfig = rpUserPayConfigService.getByPayKey(payKey);
        if (rpUserPayConfig == null){
            throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR,"用户支付配置有误");
        }

        String merchantNo = rpUserPayConfig.getUserNo();//商户编号
        RpUserInfo rpUserInfo = rpUserInfoService.getDataByMerchentNo(merchantNo);
        if (rpUserInfo == null){
            throw new UserBizException(UserBizException.USER_IS_NULL,"用户不存在");
        }

        List<RpPayWay> payWayList = rpPayWayService.listByProductCode(rpUserPayConfig.getProductCode());
        if (payWayList == null || payWayList.size() <= 0){
            throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR,"支付产品配置有误");
        }

        RpTradePaymentOrder rpTradePaymentOrder = rpTradePaymentOrderDao.selectByMerchantNoAndMerchantOrderNo(merchantNo, bo.getOrderNo());
        if (rpTradePaymentOrder == null){
        	        	
        	//String merchantNo, String merchantName, String payWay, String payWayName , PayTypeEnum payType, String fundIntoType, OrderPayBo bo)
            rpTradePaymentOrder = sealRpTradePaymentOrder(merchantNo,  rpUserInfo.getUserName(), null, null ,null , rpUserPayConfig.getFundIntoType(), bo);
            rpTradePaymentOrderDao.insert(rpTradePaymentOrder);
        }else{

            if (TradeStatusEnum.SUCCESS.name().equals(rpTradePaymentOrder.getStatus())){
                throw new TradeBizException(TradeBizException.TRADE_ORDER_ERROR,"订单已支付成功,无需重复支付");
            }

            if (rpTradePaymentOrder.getOrderAmount().compareTo(bo.getOrderPrice()) != 0 ){
                rpTradePaymentOrder.setOrderAmount(bo.getOrderPrice());//如果金额不一致,修改金额为最新的金额
                rpTradePaymentOrderDao.update(rpTradePaymentOrder);
            }
        }

        RpPayGateWayPageShowVo payGateWayPageShowVo = new RpPayGateWayPageShowVo();
        payGateWayPageShowVo.setProductName(rpTradePaymentOrder.getProductName());//产品名称
        payGateWayPageShowVo.setMerchantName(rpTradePaymentOrder.getMerchantName());//商户名称
        payGateWayPageShowVo.setOrderAmount(rpTradePaymentOrder.getOrderAmount());//订单金额
        payGateWayPageShowVo.setMerchantOrderNo(rpTradePaymentOrder.getMerchantOrderNo());//商户订单号
        payGateWayPageShowVo.setPayKey(payKey);//商户支付key

        Map<String , PayWayEnum> payWayEnumMap = new HashMap<String , PayWayEnum>();
        for (RpPayWay payWay :payWayList){
            payWayEnumMap.put(payWay.getPayWayCode(), PayWayEnum.getEnum(payWay.getPayWayCode()));
        }

        payGateWayPageShowVo.setPayWayEnumMap(payWayEnumMap);

        return payGateWayPageShowVo;

    }

    /**
     * 非直连扫码支付,选择支付方式后,去支付
     * @param payKey
     * @param orderNo
     * @param payWayCode
     * @return
     */
    @Override
    public ScanPayResultVo toNonDirectScanPay(String payKey , String orderNo, String payWayCode) {

        RpUserPayConfig rpUserPayConfig = rpUserPayConfigService.getByPayKey(payKey);
        if (rpUserPayConfig == null){
            throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR,"用户支付配置有误");
        }

        //根据支付产品及支付方式获取费率
        RpPayWay payWay = null;
        if (PayWayEnum.WEIXIN.name().equals(payWayCode)){
            payWay = rpPayWayService.getByPayWayTypeCode(rpUserPayConfig.getProductCode(), payWayCode, PayTypeEnum.SCANPAY.name());
        }else if (PayWayEnum.ALIPAY.name().equals(payWayCode)){
            payWay = rpPayWayService.getByPayWayTypeCode(rpUserPayConfig.getProductCode(), payWayCode, PayTypeEnum.DIRECT_PAY.name());
        }

        if(payWay == null){
            throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR,"用户支付配置有误");
        }

        String merchantNo = rpUserPayConfig.getUserNo();//商户编号
        RpUserInfo rpUserInfo = rpUserInfoService.getDataByMerchentNo(merchantNo);
        if (rpUserInfo == null){
            throw new UserBizException(UserBizException.USER_IS_NULL,"用户不存在");
        }

        //根据商户订单号获取订单信息
        RpTradePaymentOrder rpTradePaymentOrder = rpTradePaymentOrderDao.selectByMerchantNoAndMerchantOrderNo(merchantNo, orderNo);
        if (rpTradePaymentOrder == null){
            throw new TradeBizException(TradeBizException.TRADE_ORDER_ERROR,"订单不存在");
        }

        if (TradeStatusEnum.SUCCESS.name().equals(rpTradePaymentOrder.getStatus())){
            throw new TradeBizException(TradeBizException.TRADE_ORDER_ERROR,"订单已支付成功,无需重复支付");
        }

        return getScanPayResultVo(rpTradePaymentOrder , payWay);

    }


    /**
     * 通过支付订单及商户费率生成支付记录
     * @param rpTradePaymentOrder   支付订单
     * @param payWay   商户支付配置
     * @return
     */
    private ScanPayResultVo getScanPayResultVo(RpTradePaymentOrder rpTradePaymentOrder ,RpPayWay payWay){

        ScanPayResultVo scanPayResultVo = new ScanPayResultVo();

        String payWayCode = payWay.getPayWayCode();//支付方式

        PayTypeEnum payType = null;
        if (PayWayEnum.WEIXIN.name().equals(payWay.getPayWayCode())){
            payType = PayTypeEnum.SCANPAY;
        }else if(PayWayEnum.ALIPAY.name().equals(payWay.getPayWayCode())){
            payType = PayTypeEnum.DIRECT_PAY;
        }

        rpTradePaymentOrder.setPayTypeCode(payType.name());
        rpTradePaymentOrder.setPayTypeName(payType.getDesc());

        rpTradePaymentOrder.setPayWayCode(payWay.getPayWayCode());
        rpTradePaymentOrder.setPayWayName(payWay.getPayWayName());
        rpTradePaymentOrderDao.update(rpTradePaymentOrder);
        
        OrderPayBo bo = this.changToOrderByBo(rpTradePaymentOrder);

        RpTradePaymentRecord rpTradePaymentRecord = sealRpTradePaymentRecord(
        		rpTradePaymentOrder.getMerchantNo(),  rpTradePaymentOrder.getMerchantName() , 
        		payWay.getPayWayCode(),  payWay.getPayWayName() , payType, 
        		rpTradePaymentOrder.getFundIntoType(), BigDecimal.valueOf(payWay.getPayRate()),  bo);
        rpTradePaymentRecordDao.insert(rpTradePaymentRecord);

		LOG.info("支付方式={}", payWayCode);
		LOG.info("支付类型={}", payType.getDesc());
        
        if (PayWayEnum.WEIXIN.name().equals(payWayCode)){//微信支付
            String appid = "";
            String mch_id = "";
            String partnerKey = "";
            if (FundInfoTypeEnum.MERCHANT_RECEIVES.name().equals(rpTradePaymentOrder.getFundIntoType())){//商户收款
                //根据资金流向获取配置信息
                RpUserPayInfo rpUserPayInfo = rpUserPayInfoService.getByUserNo(rpTradePaymentOrder.getMerchantNo(),payWayCode);
                appid = rpUserPayInfo.getAppId();
                mch_id = rpUserPayInfo.getMerchantId();
                partnerKey = rpUserPayInfo.getPartnerKey();
            }else if (FundInfoTypeEnum.PLAT_RECEIVES.name().equals(rpTradePaymentOrder.getFundIntoType())){//平台收款
                appid = PropertyConfigurer.getString("weixinpay.appId");
                mch_id = PropertyConfigurer.getString("weixinpay.mch_id");
                partnerKey = PropertyConfigurer.getString("weixinpay.partnerKey");
            }

            WeiXinPrePay weiXinPrePay = sealWeixinPerPay(appid , mch_id , 
            		rpTradePaymentOrder.getProductName() ,rpTradePaymentOrder.getRemark() , 
            		rpTradePaymentRecord.getBankOrderNo() , rpTradePaymentOrder.getOrderAmount() ,  
            		rpTradePaymentOrder.getOrderTime() ,  rpTradePaymentOrder.getOrderPeriod() , 
            		WeiXinTradeTypeEnum.NATIVE ,
                    rpTradePaymentRecord.getBankOrderNo() ,"" ,rpTradePaymentOrder.getOrderIp());
            String prePayXml = WeiXinPayUtils.getPrePayXml(weiXinPrePay, partnerKey);
            //调用微信支付的功能,获取微信支付code_url
            
            String prepayUrl = PropertyConfigurer.getString("weixinpay.prepay_url");
            Map<String, Object> prePayRequest = WeiXinPayUtils.httpXmlRequest(prepayUrl, "POST", prePayXml);
            if (WeixinTradeStateEnum.SUCCESS.name().equals(prePayRequest.get("return_code")) && WeixinTradeStateEnum.SUCCESS.name().equals(prePayRequest.get("result_code"))) {
                String weiXinPrePaySign = WeiXinPayUtils.geWeiXintPrePaySign(appid, mch_id, weiXinPrePay.getDeviceInfo(), WeiXinTradeTypeEnum.NATIVE.name(), prePayRequest, partnerKey);
                String codeUrl = String.valueOf(prePayRequest.get("code_url"));
                LOG.info("预支付生成成功,{}",codeUrl);
                if (prePayRequest.get("sign").equals(weiXinPrePaySign)) {
                    rpTradePaymentRecord.setBankReturnMsg(prePayRequest.toString());
                    rpTradePaymentRecordDao.update(rpTradePaymentRecord);
                    scanPayResultVo.setCodeUrl(codeUrl);//设置微信跳转地址
                    scanPayResultVo.setPayWayCode(PayWayEnum.WEIXIN.name());
                    scanPayResultVo.setProductName(rpTradePaymentOrder.getProductName());
                    scanPayResultVo.setOrderAmount(rpTradePaymentOrder.getOrderAmount());
                    
                    //再次前面返回给APP
                    Map<String, String> prePay = WeiXinPayUtils.getPrePayMapForAPP(prePayRequest, partnerKey);
                    scanPayResultVo.setPrePay(prePay);
                }else{
                    throw new TradeBizException(TradeBizException.TRADE_WEIXIN_ERROR,"微信返回结果签名异常");
                }
            }else{
                throw new TradeBizException(TradeBizException.TRADE_WEIXIN_ERROR,"请求微信异常");
            }
        }else if (PayWayEnum.ALIPAY.name().equals(payWayCode)){//支付宝支付
        	String partner = "";
        	String sellerId = "";
        	String sectet = "";
        	if (FundInfoTypeEnum.MERCHANT_RECEIVES.name().equals(rpTradePaymentOrder.getFundIntoType())){//商户收款
                //根据资金流向获取配置信息
                RpUserPayInfo rpUserPayInfo = rpUserPayInfoService.getByUserNo(rpTradePaymentOrder.getMerchantNo(),payWayCode);
                partner = rpUserPayInfo.getPartnerKey();
                sellerId = rpUserPayInfo.getMerchantId();
                sectet = rpUserPayInfo.getAppSectet();
            }else if (FundInfoTypeEnum.PLAT_RECEIVES.name().equals(rpTradePaymentOrder.getFundIntoType())){//平台收款
            	partner = AlipayConfigUtil.partner;
            	sellerId = AlipayConfigUtil.seller_id;
            	sectet = AlipayConfigUtil.key;
            }
        	LOG.info("支付宝partner={}", partner);
        	LOG.info("支付宝sellerId={}", sellerId);
        	LOG.info("支付宝sectet={}", sectet);
            //把请求参数打包成数组
            Map<String, String> sParaTemp = new HashMap<String, String>();
            sParaTemp.put("service", AlipayConfigUtil.service);
            sParaTemp.put("partner", partner);
            sParaTemp.put("seller_id", sellerId);
            sParaTemp.put("_input_charset", AlipayConfigUtil.input_charset);
            sParaTemp.put("payment_type", AlipayConfigUtil.payment_type);
            sParaTemp.put("notify_url", AlipayConfigUtil.notify_url);
            sParaTemp.put("return_url", AlipayConfigUtil.return_url);
            sParaTemp.put("anti_phishing_key", AlipayConfigUtil.anti_phishing_key);
            sParaTemp.put("exter_invoke_ip", AlipayConfigUtil.exter_invoke_ip);
            sParaTemp.put("out_trade_no", rpTradePaymentRecord.getBankOrderNo());
            sParaTemp.put("subject", rpTradePaymentOrder.getProductName());
            sParaTemp.put("total_fee", String.valueOf(rpTradePaymentOrder.getOrderAmount().setScale(2,BigDecimal.ROUND_HALF_UP)));//小数点后两位
            sParaTemp.put("body", "");
            //获取请求页面数据
            String sHtmlText = AlipaySubmit.buildRequest(sParaTemp, sectet, "post", "确认");
            LOG.info("支付宝sHtmlText={}", sHtmlText);
            rpTradePaymentRecord.setBankReturnMsg(sHtmlText);
            rpTradePaymentRecordDao.update(rpTradePaymentRecord);
            scanPayResultVo.setCodeUrl(sHtmlText);//设置跳转地址
            scanPayResultVo.setPayWayCode(PayWayEnum.ALIPAY.name());
            scanPayResultVo.setProductName(rpTradePaymentOrder.getProductName());
            scanPayResultVo.setOrderAmount(rpTradePaymentOrder.getOrderAmount());

        }else{
            throw new TradeBizException(TradeBizException.TRADE_PAY_WAY_ERROR,"错误的支付方式");
        }

        return scanPayResultVo;
    }
    
    private OrderPayBo changToOrderByBo(RpTradePaymentOrder rpTradePaymentOrder) {
    	OrderPayBo orderPay = new OrderPayBo();
    	orderPay.setField1(rpTradePaymentOrder.getField1());
    	orderPay.setField2(rpTradePaymentOrder.getField2());
    	orderPay.setField3(rpTradePaymentOrder.getField3());
    	orderPay.setField4(rpTradePaymentOrder.getField4());
    	orderPay.setField5(rpTradePaymentOrder.getField5());
    	orderPay.setNotifyUrl(rpTradePaymentOrder.getNotifyUrl());
    	orderPay.setReturnUrl(rpTradePaymentOrder.getReturnUrl());
    	orderPay.setOrderDate(rpTradePaymentOrder.getOrderDate());
    	orderPay.setOrderIp(rpTradePaymentOrder.getOrderIp());
    	orderPay.setOrderNo(rpTradePaymentOrder.getMerchantOrderNo());
    	orderPay.setOrderPeriod(rpTradePaymentOrder.getOrderPeriod());
    	orderPay.setOrderPrice(rpTradePaymentOrder.getOrderAmount());
    	orderPay.setOrderTime(rpTradePaymentOrder.getOrderTime());
    	
    	orderPay.setProductName(rpTradePaymentOrder.getProductName());
    	orderPay.setRemark(rpTradePaymentOrder.getRemark());
    	
    	return orderPay;
    }
    
    /**
     * 通过支付订单及商户费率生成支付记录
     * @param rpTradePaymentOrder   支付订单
     * @param payWay   商户支付配置
     * @return
     */
    private AppPayResultVo getAppPayResultVo(RpTradePaymentOrder rpTradePaymentOrder, RpPayWay payWay, PayTypeEnum payType){

    	AppPayResultVo appPayResultVo = new AppPayResultVo();

        String payWayCode = payWay.getPayWayCode();//支付方式
        
        LOG.info("支付方式:" + payWayCode);

        rpTradePaymentOrder.setPayTypeCode(payType.name());
        rpTradePaymentOrder.setPayTypeName(payType.getDesc());

        rpTradePaymentOrder.setPayWayCode(payWay.getPayWayCode());
        rpTradePaymentOrder.setPayWayName(payWay.getPayWayName());
        rpTradePaymentOrderDao.update(rpTradePaymentOrder);
        
        OrderPayBo bo = this.changToOrderByBo(rpTradePaymentOrder);

        RpTradePaymentRecord rpTradePaymentRecord = sealRpTradePaymentRecord(
        		rpTradePaymentOrder.getMerchantNo(),  rpTradePaymentOrder.getMerchantName(), 
        		payWay.getPayWayCode(),  payWay.getPayWayName() , payType, rpTradePaymentOrder.getFundIntoType(), 
        		BigDecimal.valueOf(payWay.getPayRate()), bo);
        
        rpTradePaymentRecordDao.insert(rpTradePaymentRecord);

        if (PayWayEnum.WEIXIN.name().equals(payWayCode)){//微信支付
            String appid = "";
            String mch_id = "";
            String partnerKey = "";
            if (FundInfoTypeEnum.MERCHANT_RECEIVES.name().equals(rpTradePaymentOrder.getFundIntoType())){//商户收款
                //根据资金流向获取配置信息
                RpUserPayInfo rpUserPayInfo = rpUserPayInfoService.getByUserNo(rpTradePaymentOrder.getMerchantNo(), payWayCode);
                appid = rpUserPayInfo.getAppId();
                mch_id = rpUserPayInfo.getMerchantId();
                partnerKey = rpUserPayInfo.getPartnerKey();
            }else if (FundInfoTypeEnum.PLAT_RECEIVES.name().equals(rpTradePaymentOrder.getFundIntoType())){//平台收款
                appid = PropertyConfigurer.getString("weixinpay.appId");
                mch_id = PropertyConfigurer.getString("weixinpay.mch_id");
                partnerKey = PropertyConfigurer.getString("weixinpay.partnerKey");
            }
            
            String subMerchantId = rpTradePaymentOrder.getSubMerchantNo();
			//服务商支付
			if (null != subMerchantId) {

				WxPayConfigStorage wxPayConfigStorage = new WxPayConfigStorage();
				wxPayConfigStorage.setMchId(mch_id);
				wxPayConfigStorage.setAppId(appid);
				wxPayConfigStorage.setSubMchId(subMerchantId);
				//wxPayConfigStorage.setKeyPublic("转账公钥，转账时必填");
				wxPayConfigStorage.setSecretKey(partnerKey);
				wxPayConfigStorage.setNotifyUrl(PropertyConfigurer.getString("weixinpay.notify_url"));
				wxPayConfigStorage.setReturnUrl(PropertyConfigurer.getString("weixinpay.notify_url"));
				wxPayConfigStorage.setSignType("md5");
				wxPayConfigStorage.setInputCharset("utf-8");
				
		        //支付服务
		        PayService service =  new WxPayService(wxPayConfigStorage);
		        
		        
		        BigDecimal totalFee = rpTradePaymentOrder.getOrderAmount().multiply(BigDecimal.valueOf(100d));
				// 支付订单基础信息
				//String subject, String body, BigDecimal price, String outTradeNo
				PayOrder payOrder = new PayOrder(rpTradePaymentOrder.getProductName(), 
						rpTradePaymentOrder.getRemark(), totalFee, rpTradePaymentRecord.getBankOrderNo());

				if(payType.name().equals(PayTypeEnum.WX_PROGRAM_PAY.name())) {
			        //公众号支付
			        payOrder.setTransactionType(WxTransactionType.JSAPI);
			        //微信公众号对应微信付款用户的唯一标识
			        payOrder.setOpenid(rpTradePaymentOrder.getField5());
			        Map<String, Object> appOrderInfo = service.orderInfo(payOrder);
			        System.out.println(appOrderInfo);
			          
                }
                else {
                    payOrder.setTransactionType(WxTransactionType.APP);
                    //获取APP支付所需的信息组，直接给app端就可使用
                    Map<String, Object> appOrderInfo = service.orderInfo(payOrder);
                    System.out.println(appOrderInfo);
                }
				

			} else {
				WeiXinTradeTypeEnum wxTradeType = WeiXinTradeTypeEnum.APP;
	            if(payType.name().equals(PayTypeEnum.WX_PROGRAM_PAY.name())) {
	            	wxTradeType = WeiXinTradeTypeEnum.JSAPI;
	            }

	            WeiXinPrePay weiXinPrePay = sealWeixinPerPay(appid , mch_id , 
	            		rpTradePaymentOrder.getProductName() ,rpTradePaymentOrder.getRemark() , 
	            		rpTradePaymentRecord.getBankOrderNo() , rpTradePaymentOrder.getOrderAmount() ,  
	            		rpTradePaymentOrder.getOrderTime() ,  rpTradePaymentOrder.getOrderPeriod() , 
	            		wxTradeType, rpTradePaymentRecord.getBankOrderNo() ,rpTradePaymentOrder.getField5(),
	            		rpTradePaymentOrder.getOrderIp());
	                       
	            String prePayXml = WeiXinPayUtils.getPrePayXml(weiXinPrePay, partnerKey);
	            //调用微信支付的功能,获取微信支付code_url
	            
	            String prepayUrl = PropertyConfigurer.getString("weixinpay.prepay_url");
	            Map<String, Object> prePayRequest = WeiXinPayUtils.httpXmlRequest(prepayUrl, "POST", prePayXml);
	            if (WeixinTradeStateEnum.SUCCESS.name().equals(prePayRequest.get("return_code")) 
	            		&& WeixinTradeStateEnum.SUCCESS.name().equals(prePayRequest.get("result_code"))) {
	                String weiXinPrePaySign = WeiXinPayUtils.geWeiXintPrePaySign(appid, mch_id, 
	                		weiXinPrePay.getDeviceInfo(), wxTradeType.name(), prePayRequest, partnerKey);
	                if (prePayRequest.get("sign").equals(weiXinPrePaySign)) {
	                    rpTradePaymentRecord.setBankReturnMsg(prePayRequest.toString());
	                    rpTradePaymentRecordDao.update(rpTradePaymentRecord);
	                    appPayResultVo.setPayWayCode(PayWayEnum.WEIXIN.name());
	                    appPayResultVo.setProductName(rpTradePaymentOrder.getProductName());
	                    appPayResultVo.setOrderAmount(rpTradePaymentOrder.getOrderAmount());
	                    
	                    if(payType.name().equals(PayTypeEnum.WX_PROGRAM_PAY.name())) {
	                        //再次前面返回给APP
	                        Map<String, String> prePay = WeiXinPayUtils.getPrePayMapForJSAPI(prePayRequest, partnerKey);
	                        appPayResultVo.setPrePay(prePay);
	                    }
	                    else {
	                        //再次前面返回给APP
	                        Map<String, String> prePay = WeiXinPayUtils.getPrePayMapForAPP(prePayRequest, partnerKey);
	                        appPayResultVo.setPrePay(prePay);
	                    }
	                } else {
	                    throw new TradeBizException(TradeBizException.TRADE_WEIXIN_ERROR,"微信返回结果签名异常");
	                }
	            } else {
	            	LOG.error(prePayRequest.toString());
	                throw new TradeBizException(TradeBizException.TRADE_WEIXIN_ERROR,"请求微信异常");
	            }
			}
            
            
		} else if (PayWayEnum.ALIPAY.name().equals(payWayCode)) {
			// 支付宝支付
			String app_id = "";
			String merchant_id = "";
			String rsa_private_key = "";
			String alipay_public_key = "";

			if (FundInfoTypeEnum.MERCHANT_RECEIVES.name().equals(rpTradePaymentOrder.getFundIntoType())) {// 商户收款
				// 根据资金流向获取配置信息
				RpUserPayInfo rpUserPayInfo = rpUserPayInfoService.getByUserNo(rpTradePaymentOrder.getMerchantNo(), payWayCode);
				app_id = rpUserPayInfo.getAppId();
				merchant_id = rpUserPayInfo.getMerchantId();
				rsa_private_key = rpUserPayInfo.getRsaPrivateKey();
				alipay_public_key = rpUserPayInfo.getRsaPublicKey();
			} else if (FundInfoTypeEnum.PLAT_RECEIVES.name().equals(rpTradePaymentOrder.getFundIntoType())) {// 平台收款
				app_id = AlipayConfigUtil.app_id;
				merchant_id = AlipayConfigUtil.seller_id;
				rsa_private_key = AlipayConfigUtil.rsa_private_key;
				alipay_public_key = AlipayConfigUtil.rsa_public_key;
			}

			String charset = AlipayConfigUtil.input_charset;
			String serverUrl = "https://openapi.alipay.com/gateway.do";
			BigDecimal total_amount = rpTradePaymentOrder.getOrderAmount().setScale(2, BigDecimal.ROUND_HALF_UP);

			AlipayClient alipayClient = new 
					DefaultAlipayClient(serverUrl, app_id, rsa_private_key, "json", charset, alipay_public_key, "RSA2");
			
			String prePayMessage = null;
			String seller_id = merchant_id;

			String subMerchantId = bo.getSubMerchantNo();
			
			if(null != subMerchantId) {
				seller_id = subMerchantId.trim();
			}
			
			AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
			AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
			// model.setBody("我是测试数据");
			model.setSellerId(seller_id);
			model.setSubject(rpTradePaymentOrder.getProductName());
			model.setOutTradeNo(rpTradePaymentRecord.getBankOrderNo());
			model.setTimeoutExpress("30m");
			model.setTotalAmount(String.valueOf(total_amount));
			model.setProductCode("QUICK_MSECURITY_PAY");
			request.setBizModel(model);
			request.setNotifyUrl(AlipayConfigUtil.notify_url);
			
			if(null != subMerchantId) {
				RpUserPayOauth oauth = rpUserPayOauthService.getOne("ALIPAY", app_id, merchant_id, seller_id);
				if (null != oauth) {
					ExtendParams extendParams = new ExtendParams();
					extendParams.setSysServiceProviderId(merchant_id.trim());
					model.setExtendParams(extendParams);

					// 获取授权token
					String authToken = oauth.getAuthToken();
					if (StringUtils.isEmpty(authToken)) {
						LOG.error("alipay_支付宝获取authToken失败，sellerId=" + bo.getSubMerchantNo());
						return null;
					}
					request.putOtherTextParam("app_auth_token", authToken.trim());
				}
			}

			try {
				AlipayTradeAppPayResponse response = alipayClient.sdkExecute(request);
				if(response.isSuccess()){
					prePayMessage = response.getBody();
				} else {
					LOG.error("AlipayTradeAppPayResponse Error!");
				}
				LOG.info(prePayMessage);
			} catch (AlipayApiException e) {
				LOG.info(e.getMessage());
			}

			rpTradePaymentRecord.setBankReturnMsg(prePayMessage);
			rpTradePaymentRecordDao.update(rpTradePaymentRecord);
			// appPayResultVo.setCodeUrl(sHtmlText);//设置微信跳转地址
			appPayResultVo.setPayWayCode(PayWayEnum.ALIPAY.name());
			appPayResultVo.setProductName(rpTradePaymentOrder.getProductName());
			appPayResultVo.setOrderAmount(rpTradePaymentOrder.getOrderAmount());
			//appPayResultVo.setPrePay(prePayMessage.replaceAll("&", "&amp;"));
			appPayResultVo.setPayBody(prePayMessage);
			Map<String, String> prePay = UrlMapUtility.splitAndDecode(prePayMessage);
			prePay.put("seller_id", seller_id);
			appPayResultVo.setPrePay(prePay);

		} else if (PayWayEnum.APPLE.name().equals(payWayCode)){//苹果支付
            Map<String, String> sParaTemp = new HashMap<String, String>();
            sParaTemp.put("out_trade_no", rpTradePaymentRecord.getBankOrderNo());
            appPayResultVo.setPayWayCode(PayWayEnum.APPLE.name());
            appPayResultVo.setProductName(rpTradePaymentOrder.getProductName());
            appPayResultVo.setOrderAmount(rpTradePaymentOrder.getOrderAmount());
            appPayResultVo.setPrePay(sParaTemp);
        } else{
            throw new TradeBizException(TradeBizException.TRADE_PAY_WAY_ERROR,"错误的支付方式");
        }

        return appPayResultVo;
    }

    /**
     * 完成扫码支付(支付宝即时到账支付)
     * @param payWayCode
     * @param notifyMap
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String completeScanPay(String payWayCode ,Map<String, String> notifyMap) {
		LOG.info("接收到{}支付结果{}", payWayCode, notifyMap);

		String returnStr = null;
		String bankOrderNo = notifyMap.get("out_trade_no");
		// 根据银行订单号获取支付信息
		LOG.info("bankOrderNo=" + bankOrderNo);
		
		if (StringUtil.isEmpty(bankOrderNo)) {
			LOG.info("参数out_trade_no异常" + TradeBizException.TRADE_PARAM_ERROR);
			throw new TradeBizException(TradeBizException.TRADE_PARAM_ERROR, "参数out_trade_no异常");
		}
		RpTradePaymentRecord rpTradePaymentRecord = rpTradePaymentRecordDao.getByBankOrderNo(bankOrderNo);
		if (rpTradePaymentRecord == null) {
			LOG.info("非法订单,订单不存在" + TradeBizException.TRADE_ORDER_ERROR);
			throw new TradeBizException(TradeBizException.TRADE_ORDER_ERROR, "非法订单,订单不存在");
		}

		LOG.info("rpTradePaymentRecord=" + rpTradePaymentRecord.toString());
		if (TradeStatusEnum.SUCCESS.name().equals(rpTradePaymentRecord.getStatus())) {
			throw new TradeBizException(TradeBizException.TRADE_ORDER_ERROR, "订单为成功状态");
		}
		String merchantNo = rpTradePaymentRecord.getMerchantNo();// 商户编号

		LOG.info("merchantNo=" + merchantNo);
		// 根据支付订单获取配置信息
		String fundIntoType = rpTradePaymentRecord.getFundIntoType();// 获取资金流入类型
		String partnerKey = "";
		RpUserPayInfo rpUserPayInfo = null;

		if (FundInfoTypeEnum.MERCHANT_RECEIVES.name().equals(fundIntoType)) {// 商户收款
			// 根据资金流向获取配置信息
			rpUserPayInfo = rpUserPayInfoService.getByUserNo(merchantNo, payWayCode);
			partnerKey = rpUserPayInfo.getPartnerKey();

			LOG.info("rpUserPayInfo=" + rpUserPayInfo.toString());
		} else if (FundInfoTypeEnum.PLAT_RECEIVES.name().equals(fundIntoType)) {// 平台收款
			RpUserPayConfig rpUserPayConfig = rpUserPayConfigService.getByUserNo(merchantNo);
			if (rpUserPayConfig == null) {
				throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR, "用户支付配置有误");
			}
			// 根据支付产品及支付方式获取费率
			RpPayWay payWay = rpPayWayService.getByPayWayTypeCode(rpUserPayConfig.getProductCode(),
					rpTradePaymentRecord.getPayWayCode(), rpTradePaymentRecord.getPayTypeCode());
			if (payWay == null) {
				throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR, "用户支付配置有误");
			}
		} else {
			LOG.error("错误的收款方式:" + fundIntoType);
		}

		if (PayWayEnum.WEIXIN.name().equals(payWayCode)) {
			String sign = notifyMap.remove("sign");
			LOG.info("sign=" + sign);
			if (FundInfoTypeEnum.PLAT_RECEIVES.name().equals(fundIntoType)) {
				partnerKey = PropertyConfigurer.getString("weixinpay.partnerKey");
			}

			if (WeiXinPayUtils.notifySign(notifyMap, sign, partnerKey)) {// 根据配置信息验证签名
				if (WeixinTradeStateEnum.SUCCESS.name().equals(notifyMap.get("result_code"))) {// 业务结果成功
					String timeEndStr = notifyMap.get("time_end");
					Date timeEnd = null;
					if (!StringUtil.isEmpty(timeEndStr)) {
						timeEnd = DateUtils.getDateFromString(timeEndStr, "yyyyMMddHHmmss");// 订单支付完成时间
					}
					completeSuccessOrder(rpTradePaymentRecord, notifyMap.get("transaction_id"), timeEnd, notifyMap.toString());
					returnStr = "<xml>\n" 
							+ "  <return_code><![CDATA[SUCCESS]]></return_code>\n"
							+ "  <return_msg><![CDATA[OK]]></return_msg>\n" 
							+ "</xml>";
					LOG.info("returnStr=" + returnStr);
				} else {
					completeFailOrder(rpTradePaymentRecord, notifyMap.toString());
				}
			} else {
				throw new TradeBizException(TradeBizException.TRADE_WEIXIN_ERROR, "微信签名失败");
			}
		} else if (PayWayEnum.ALIPAY.name().equals(payWayCode)) {
			String signType = notifyMap.get("sign_type");
			String decryptKey = null;
			if (FundInfoTypeEnum.MERCHANT_RECEIVES.name().equals(fundIntoType)) {// 商户收款
				if (signType.equalsIgnoreCase("MD5")) {
					decryptKey = rpUserPayInfo.getAppSectet();
				} else if (signType.equalsIgnoreCase("RSA") || signType.equalsIgnoreCase("RSA2")) {
					decryptKey = rpUserPayInfo.getRsaPublicKey();
				} else {
					LOG.error("错误的加密方式:" + signType);
				}
				LOG.info("MERCHANT_RECEIVES -> signType:" + signType);
			} else if (FundInfoTypeEnum.PLAT_RECEIVES.name().equals(fundIntoType)) {// 平台收款
				if (signType.equalsIgnoreCase("MD5")) {
					decryptKey = AlipayConfigUtil.key;
				} else if (signType.equalsIgnoreCase("RSA") || signType.equalsIgnoreCase("RSA2")) {
					decryptKey = AlipayConfigUtil.rsa_public_key;
				} else {
					LOG.error("错误的加密方式:" + signType);
				}
				LOG.info("PLAT_RECEIVES -> signType:" + signType);
			} else {
				LOG.error("错误的收款方式:" + fundIntoType);
			}
			
			LOG.info("Pre AlipayNotify:" + notifyMap);
			
			String charset = AlipayConfigUtil.input_charset;
			LOG.info("sign charset:" + charset);
			boolean verifyResult = AlipayNotify.verify(partnerKey, decryptKey, notifyMap);
			LOG.info("verifyResult:" + verifyResult);
			if (verifyResult) {// 验证成功
				String tradeStatus = notifyMap.get("trade_status");
				LOG.info("AFT AlipayNotify:tradeStatus=" + tradeStatus);
				if (AliPayTradeStateEnum.TRADE_FINISHED.name().equals(tradeStatus)) {
					// 判断该笔订单是否在商户网站中已经做过处理
					// 如果没有做过处理，根据订单号（out_trade_no）在商户网站的订单系统中查到该笔订单的详细，并执行商户的业务程序
					// 请务必判断请求时的total_fee、seller_id与通知时获取的total_fee、seller_id为一致的
					// 如果有做过处理，不执行商户的业务程序

					// 注意：
					// 退款日期超过可退款期限后（如三个月可退款），支付宝系统发送该交易状态通知
				} else if (AliPayTradeStateEnum.TRADE_SUCCESS.name().equals(tradeStatus)) {
					String gmtPaymentStr = notifyMap.get("gmt_payment");// 付款时间
					Date timeEnd = null;
					if (!StringUtil.isEmpty(gmtPaymentStr)) {
						timeEnd = DateUtils.getDateFromString(gmtPaymentStr, "yyyy-MM-dd HH:mm:ss");
					}
					LOG.info("Pre completeSuccessOrder:" + notifyMap.get("trade_no"));
					completeSuccessOrder(rpTradePaymentRecord, notifyMap.get("trade_no"), timeEnd, notifyMap.toString());
					returnStr = "success";
				} else {
					completeFailOrder(rpTradePaymentRecord, notifyMap.toString());
					returnStr = "fail";
				}
			} else {// 验证失败
				throw new TradeBizException(TradeBizException.TRADE_ALIPAY_ERROR, "支付宝签名异常");
			}
		} else if (PayWayEnum.APPLE.name().equals(payWayCode)) {
			//苹果客户端传上来的收据,是最原据的收据  
			String receipt = notifyMap.get("receipt");
			Boolean isSandbox = PropertyConfigurer.getBoolean("applepay.sandbox", true);
			String verifyResult = ApplePayNotify.buyAppVerify(receipt, isSandbox);
			
			if (verifyResult == null) {
                //苹果服务器没有返回验证结果  
            	try {
					completeFailOrder(rpTradePaymentRecord, JSONUtils.serializeObject(notifyMap));
				} catch (Exception e) {}
				throw new TradeBizException(TradeBizException.TRADE_APPLE_ERROR, "苹果签名异常");
			} else {
				//跟苹果验证有返回结果------------------ 
				AppleVerifyBean verifybean = ApplePayUtil.parseJsonToBean(verifyResult);
				String states = verifybean.getStatus().toString();
				if (states.equals("0")) {// 验证成功
					
					AppleReceiptBean receiptBean = verifybean.getReceipt();
					String timeEndStr = receiptBean.getReceipt_creation_date();
					Date timeEnd = null;
					if (!StringUtil.isEmpty(timeEndStr)) {
						timeEnd = DateUtils.getDateFromString(timeEndStr, "yyyy-MM-dd HH:mm:ss 'Etc/GMT'");// 订单支付完成时间
					}
					
					String transactionId = ApplePayUtil.getTransactionId(receiptBean);
					completeSuccessOrder(rpTradePaymentRecord, transactionId, timeEnd, verifyResult.toString());
					returnStr = "{\"message\": \"success\",\"code\": 0}";
					LOG.info("returnStr=" + returnStr);
				} else {
					// 账单无效
					completeFailOrder(rpTradePaymentRecord, verifyResult);
					throw new TradeBizException(TradeBizException.TRADE_APPLE_ERROR, "苹果验证异常");
				}
                //跟苹果验证有返回结果------------------  
            }  
			
		} else {
			throw new TradeBizException(TradeBizException.TRADE_PAY_WAY_ERROR, "错误的支付方式");
		}

		LOG.info("返回支付通道{}信息{}", payWayCode, returnStr);
        return returnStr;
    }

    /**
     * 支付成功后,又是会出现页面通知早与后台通知
     * 现页面通知,暂时不做数据处理功能,只生成页面通知URL
     * @param payWayCode
     * @param resultMap
     * @return
     */
    @Override
    public OrderPayResultVo completeScanPayByResult(String payWayCode, Map<String, String> resultMap) {

        OrderPayResultVo orderPayResultVo = new OrderPayResultVo();

        String bankOrderNo = resultMap.get("out_trade_no");
        //根据银行订单号获取支付信息
        RpTradePaymentRecord rpTradePaymentRecord = rpTradePaymentRecordDao.getByBankOrderNo(bankOrderNo);
        if (rpTradePaymentRecord == null){
            throw new TradeBizException(TradeBizException.TRADE_ORDER_ERROR,",非法订单,订单不存在");
        }

        orderPayResultVo.setOrderPrice(rpTradePaymentRecord.getOrderAmount());//订单金额
        orderPayResultVo.setProductName(rpTradePaymentRecord.getProductName());//产品名称

        RpTradePaymentOrder rpTradePaymentOrder = rpTradePaymentOrderDao
        		.selectByMerchantNoAndMerchantOrderNo(rpTradePaymentRecord.getMerchantNo(), rpTradePaymentRecord.getMerchantOrderNo());

        
        String merchantNo = rpTradePaymentRecord.getMerchantNo();// 商户编号

		LOG.info("merchantNo=" + merchantNo);
		// 根据支付订单获取配置信息
		String fundIntoType = rpTradePaymentRecord.getFundIntoType();// 获取资金流入类型
        
		RpUserPayInfo rpUserPayInfo = null;
        String partnerKey = null;
        
		if (FundInfoTypeEnum.MERCHANT_RECEIVES.name().equals(fundIntoType)) {// 商户收款
			// 根据资金流向获取配置信息
			rpUserPayInfo = rpUserPayInfoService.getByUserNo(merchantNo, payWayCode);
			partnerKey = rpUserPayInfo.getPartnerKey();
			LOG.info("rpUserPayInfo=" + rpUserPayInfo.toString());
		} else if (FundInfoTypeEnum.PLAT_RECEIVES.name().equals(fundIntoType)) {// 平台收款
			RpUserPayConfig rpUserPayConfig = rpUserPayConfigService.getByUserNo(merchantNo);
			if (rpUserPayConfig == null) {
				throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR, "用户支付配置有误");
			}
			// 根据支付产品及支付方式获取费率
			RpPayWay payWay = rpPayWayService.getByPayWayTypeCode(rpUserPayConfig.getProductCode(),
					rpTradePaymentRecord.getPayWayCode(), rpTradePaymentRecord.getPayTypeCode());
			if (payWay == null) {
				throw new UserBizException(UserBizException.USER_PAY_CONFIG_ERRPR, "用户支付配置有误");
			}
		} else {
			LOG.error("错误的收款方式:" + fundIntoType);
		}
		
		if (PayWayEnum.ALIPAY.name().equals(payWayCode)) {
			String signType = resultMap.get("sign_type");
			String decryptKey = null;
			if (FundInfoTypeEnum.MERCHANT_RECEIVES.name().equals(fundIntoType)) {// 商户收款
				if (signType.equals("MD5")) {
					decryptKey = rpUserPayInfo.getAppSectet();
				} else if (signType.equals("RSA")) {
					decryptKey = rpUserPayInfo.getRsaPublicKey();
				} else {
					LOG.error("错误的加密方式:" + signType);
				}
			} else if (FundInfoTypeEnum.PLAT_RECEIVES.name().equals(fundIntoType)) {// 平台收款
				if (signType.equals("MD5")) {
					decryptKey = AlipayConfigUtil.key;
				} else if (signType.equals("RSA")) {
					decryptKey = AlipayConfigUtil.rsa_public_key;
				} else {
					LOG.error("错误的加密方式:" + signType);
				}
			} else {
				LOG.error("错误的收款方式:" + fundIntoType);
			}
			
			//计算得出通知验证结果
			boolean verifyResult = AlipayNotify.verify(partnerKey, decryptKey, resultMap);
			if (verifyResult) {// 验证成功
				String trade_status = resultMap.get("trade_status");
				if (trade_status.equals("TRADE_FINISHED") || trade_status.equals("TRADE_SUCCESS")) {
					String resultUrl = getMerchantNotifyUrl(rpTradePaymentRecord, rpTradePaymentOrder, rpTradePaymentRecord.getReturnUrl(), TradeStatusEnum.SUCCESS);
					orderPayResultVo.setReturnUrl(resultUrl);
					orderPayResultVo.setStatus(TradeStatusEnum.SUCCESS.name());
				} else {
					String resultUrl = getMerchantNotifyUrl(rpTradePaymentRecord, rpTradePaymentOrder, rpTradePaymentRecord.getReturnUrl(), TradeStatusEnum.FAILED);
					orderPayResultVo.setReturnUrl(resultUrl);
					orderPayResultVo.setStatus(TradeStatusEnum.FAILED.name());
				}
			} else {
				throw new TradeBizException(TradeBizException.TRADE_ALIPAY_ERROR, "支付宝签名异常");
			}
			return orderPayResultVo;
		} else if (PayWayEnum.WEIXIN.name().equals(payWayCode)) {
			String sign = resultMap.remove("sign");
			LOG.info("sign=" + sign);
			if (FundInfoTypeEnum.PLAT_RECEIVES.name().equals(fundIntoType)) {
				partnerKey = PropertyConfigurer.getString("weixinpay.partnerKey");
			}

			if (WeiXinPayUtils.notifySign(resultMap, sign, partnerKey)) {// 根据配置信息验证签名
				if (WeixinTradeStateEnum.SUCCESS.name().equals(resultMap.get("result_code"))) {// 业务结果成功
					String resultUrl = getMerchantNotifyUrl(rpTradePaymentRecord, rpTradePaymentOrder, rpTradePaymentRecord.getReturnUrl(), TradeStatusEnum.SUCCESS);
					orderPayResultVo.setReturnUrl(resultUrl);
					orderPayResultVo.setStatus(TradeStatusEnum.SUCCESS.name());
				} else {
					String resultUrl = getMerchantNotifyUrl(rpTradePaymentRecord, rpTradePaymentOrder, rpTradePaymentRecord.getReturnUrl(), TradeStatusEnum.FAILED);
					orderPayResultVo.setReturnUrl(resultUrl);
					orderPayResultVo.setStatus(TradeStatusEnum.FAILED.name());
				}
			} else {
				throw new TradeBizException(TradeBizException.TRADE_WEIXIN_ERROR, "微信签名失败");
			}
			return orderPayResultVo;
		} else {
			throw new TradeBizException(TradeBizException.TRADE_PAY_WAY_ERROR, "错误的支付方式");
		}
    }


    /**
     * 支付订单实体封装
     * @param merchantNo    商户编号
     * @param merchantName  商户名称
     * @param productName   产品名称
     * @param orderNo   商户订单号
     * @param orderDate 下单日期
     * @param orderTime 下单时间
     * @param orderPrice    订单金额
     * @param payWay    支付方式
     * @param payWayName    支付方式名称
     * @param payType   支付类型
     * @param fundIntoType  资金流入类型
     * @param orderIp   下单IP
     * @param orderPeriod   订单有效期
     * @param returnUrl 页面通知地址
     * @param notifyUrl 后台通知地址
     * @param remark    支付备注
     * @param field1    扩展字段1
     * @param field2    扩展字段2
     * @param field3    扩展字段3
     * @param field4    扩展字段4
     * @param field5    扩展字段5
     * @return
     */
    private RpTradePaymentOrder sealRpTradePaymentOrder(
    		String merchantNo, String merchantName, String payWay, String payWayName , PayTypeEnum payType, 
            String fundIntoType, OrderPayBo bo){

        RpTradePaymentOrder rpTradePaymentOrder = new RpTradePaymentOrder();
        rpTradePaymentOrder.setProductName(bo.getProductName());//商品名称
        if (StringUtil.isEmpty(bo.getOrderNo())){
            throw new TradeBizException(TradeBizException.TRADE_PARAM_ERROR,"订单号错误");
        }

        rpTradePaymentOrder.setMerchantOrderNo(bo.getOrderNo());//订单号

        if (bo.getOrderPrice() == null || bo.getOrderPrice().doubleValue() <= 0){
            throw new TradeBizException(TradeBizException.TRADE_PARAM_ERROR,"订单金额错误");
        }

        rpTradePaymentOrder.setOrderAmount(bo.getOrderPrice());//订单金额

        if (StringUtil.isEmpty(merchantName)){
            throw new TradeBizException(TradeBizException.TRADE_PARAM_ERROR,"商户名称错误");
        }
        rpTradePaymentOrder.setMerchantName(merchantName);//商户名称

        if (StringUtil.isEmpty(merchantNo)){
            throw new TradeBizException(TradeBizException.TRADE_PARAM_ERROR,"商户编号错误");
        }
        rpTradePaymentOrder.setMerchantNo(merchantNo);//商户编号

        if (bo.getOrderDate() == null){
            throw new TradeBizException(TradeBizException.TRADE_PARAM_ERROR,"下单日期错误");
        }
        rpTradePaymentOrder.setOrderDate(bo.getOrderDate());//下单日期

        if (bo.getOrderTime() == null){
            throw new TradeBizException(TradeBizException.TRADE_PARAM_ERROR,"下单时间错误");
        }
        rpTradePaymentOrder.setOrderTime(bo.getOrderTime());//下单时间
        rpTradePaymentOrder.setOrderIp(bo.getOrderIp());//下单IP
        rpTradePaymentOrder.setOrderRefererUrl("");//下单前页面

        if (StringUtil.isEmpty(bo.getReturnUrl())){
            throw new TradeBizException(TradeBizException.TRADE_PARAM_ERROR,"页面通知地址错误");
        }
        rpTradePaymentOrder.setReturnUrl(bo.getReturnUrl());//页面通知地址

        if (StringUtil.isEmpty(bo.getNotifyUrl())){
            throw new TradeBizException(TradeBizException.TRADE_PARAM_ERROR,"后台通知地址错误");
        }
        rpTradePaymentOrder.setNotifyUrl(bo.getNotifyUrl());//后台通知地址

        if (bo.getOrderPeriod() == null || bo.getOrderPeriod() <= 0){
            throw new TradeBizException(TradeBizException.TRADE_PARAM_ERROR,"订单有效期错误");
        }
        rpTradePaymentOrder.setOrderPeriod(bo.getOrderPeriod());//订单有效期

        Date expireTime = DateUtils.addMinute(bo.getOrderTime(), bo.getOrderPeriod());//订单过期时间
        rpTradePaymentOrder.setExpireTime(expireTime);//订单过期时间
        rpTradePaymentOrder.setPayWayCode(payWay);//支付通道编码
        rpTradePaymentOrder.setPayWayName(payWayName);//支付通道名称
        rpTradePaymentOrder.setStatus(TradeStatusEnum.WAITING_PAYMENT.name());//订单状态 等待支付

        if (payType != null){
            rpTradePaymentOrder.setPayTypeCode(payType.name());//支付类型
            rpTradePaymentOrder.setPayTypeName(payType.getDesc());//支付方式
        }
        rpTradePaymentOrder.setFundIntoType(fundIntoType);//资金流入方向

        rpTradePaymentOrder.setRemark(bo.getRemark());//支付备注
        rpTradePaymentOrder.setField1(bo.getField1());//扩展字段1
        rpTradePaymentOrder.setField2(bo.getField2());//扩展字段2
        rpTradePaymentOrder.setField3(bo.getField3());//扩展字段3
        rpTradePaymentOrder.setField4(bo.getField4());//扩展字段4
        rpTradePaymentOrder.setField5(bo.getField5());//扩展字段5

        return rpTradePaymentOrder;
    }


    /**
     * 封装支付流水记录实体
     * @param merchantNo    商户编号
     * @param merchantName  商户名称
     * @param productName   产品名称
     * @param orderNo   商户订单号
     * @param orderPrice    订单金额
     * @param payWay    支付方式编码
     * @param payWayName    支付方式名称
     * @param payType   支付类型
     * @param fundIntoType  资金流入方向
     * @param feeRate   支付费率
     * @param orderIp   订单IP
     * @param returnUrl 页面通知地址
     * @param notifyUrl 后台通知地址
     * @param remark    备注
     * @param field1    扩展字段1
     * @param field2    扩展字段2
     * @param field3    扩展字段3
     * @param field4    扩展字段4
     * @param field5    扩展字段5
     * @return
     */
    private RpTradePaymentRecord sealRpTradePaymentRecord(String merchantNo, String merchantName,
    	String payWay , String payWayName , PayTypeEnum payType , String fundIntoType , BigDecimal feeRate, OrderPayBo bo){
        RpTradePaymentRecord rpTradePaymentRecord = new RpTradePaymentRecord();
        rpTradePaymentRecord.setProductName(bo.getProductName());//产品名称
        rpTradePaymentRecord.setMerchantOrderNo(bo.getOrderNo());//产品编号

        String trxNo = buildNoService.buildTrxNo();
        rpTradePaymentRecord.setTrxNo(trxNo);//支付流水号

        String bankOrderNo = buildNoService.buildBankOrderNo();
        rpTradePaymentRecord.setBankOrderNo(bankOrderNo);//银行订单号
        rpTradePaymentRecord.setMerchantName(merchantName);
        rpTradePaymentRecord.setMerchantNo(merchantNo);//商户编号
        rpTradePaymentRecord.setOrderIp(bo.getOrderIp());//下单IP
        rpTradePaymentRecord.setOrderRefererUrl("");//下单前页面
        rpTradePaymentRecord.setReturnUrl(bo.getReturnUrl());//页面通知地址
        rpTradePaymentRecord.setNotifyUrl(bo.getNotifyUrl());//后台通知地址
        rpTradePaymentRecord.setPayWayCode(payWay);//支付通道编码
        rpTradePaymentRecord.setPayWayName(payWayName);//支付通道名称
        rpTradePaymentRecord.setTrxType(TrxTypeEnum.EXPENSE.name());//交易类型
        rpTradePaymentRecord.setOrderFrom(OrderFromEnum.USER_EXPENSE.name());//订单来源
        rpTradePaymentRecord.setOrderAmount(bo.getOrderPrice());//订单金额
        rpTradePaymentRecord.setStatus(TradeStatusEnum.WAITING_PAYMENT.name());//订单状态 等待支付

        rpTradePaymentRecord.setPayTypeCode(payType.name());//支付类型
        rpTradePaymentRecord.setPayTypeName(payType.getDesc());//支付方式
        rpTradePaymentRecord.setFundIntoType(fundIntoType);//资金流入方向

        if (FundInfoTypeEnum.PLAT_RECEIVES.name().equals(fundIntoType)){//平台收款 需要修改费率 成本 利润 收入 以及修改商户账户信息
            BigDecimal orderAmount = rpTradePaymentRecord.getOrderAmount();//订单金额
            BigDecimal platIncome = orderAmount.multiply(feeRate).divide(BigDecimal.valueOf(100),2,BigDecimal.ROUND_HALF_UP);  //平台收入 = 订单金额 * 支付费率(设置的费率除以100为真实费率)
            BigDecimal platCost = orderAmount.multiply(BigDecimal.valueOf(Double.valueOf(PropertyConfigurer.getString("weixinpay.pay_rate")))).divide(BigDecimal.valueOf(100),2,BigDecimal.ROUND_HALF_UP);//平台成本 = 订单金额 * 微信费率(设置的费率除以100为真实费率)
            BigDecimal platProfit = platIncome.subtract(platCost);//平台利润 = 平台收入 - 平台成本

            rpTradePaymentRecord.setFeeRate(feeRate);//费率
            rpTradePaymentRecord.setPlatCost(platCost);//平台成本
            rpTradePaymentRecord.setPlatIncome(platIncome);//平台收入
            rpTradePaymentRecord.setPlatProfit(platProfit);//平台利润

        }

        rpTradePaymentRecord.setRemark(bo.getRemark());//支付备注
        rpTradePaymentRecord.setField1(bo.getField1());//扩展字段1
        rpTradePaymentRecord.setField2(bo.getField2());//扩展字段2
        rpTradePaymentRecord.setField3(bo.getField3());//扩展字段3
        rpTradePaymentRecord.setField4(bo.getField4());//扩展字段4
        rpTradePaymentRecord.setField5(bo.getField5());//扩展字段5
        
        rpTradePaymentRecord.setSubMerchantNo(bo.getSubMerchantNo());
        
        return rpTradePaymentRecord;
    }


    /**
     * 封装预支付实体
     * @param appId 公众号ID
     * @param mchId    商户号
     * @param productName   商品描述
     * @param remark  支付备注
     * @param bankOrderNo   银行订单号
     * @param orderPrice    订单价格
     * @param orderTime 订单下单时间
     * @param orderPeriod   订单有效期
     * @param weiXinTradeTypeEnum   微信支付方式
     * @param productId 商品ID
     * @param openId    用户标识
     * @param orderIp   下单IP
     * @return
     */
    private WeiXinPrePay sealWeixinPerPay(String appId ,String mchId ,String productName ,String remark ,String bankOrderNo ,
    		BigDecimal orderPrice , Date orderTime , Integer orderPeriod,
    		WeiXinTradeTypeEnum weiXinTradeTypeEnum, String productId ,String openId ,String orderIp){
        WeiXinPrePay weiXinPrePay = new WeiXinPrePay();

        weiXinPrePay.setAppid(appId);
        weiXinPrePay.setMchId(mchId);
        weiXinPrePay.setBody(productName);//商品描述
        weiXinPrePay.setAttach(remark);//支付备注
        weiXinPrePay.setOutTradeNo(bankOrderNo);//银行订单号

        Integer totalFee = orderPrice.multiply(BigDecimal.valueOf(100d)).intValue();
        weiXinPrePay.setTotalFee(totalFee);//订单金额
        weiXinPrePay.setTimeStart(DateUtils.formatDate(orderTime, "yyyyMMddHHmmss"));//订单开始时间
        weiXinPrePay.setTimeExpire(DateUtils.formatDate(DateUtils.addMinute(orderTime, orderPeriod), "yyyyMMddHHmmss"));//订单到期时间
        weiXinPrePay.setNotifyUrl(PropertyConfigurer.getString("weixinpay.notify_url"));//通知地址
        weiXinPrePay.setTradeType(weiXinTradeTypeEnum);//交易类型
        weiXinPrePay.setProductId(productId);//商品ID
        weiXinPrePay.setOpenid(openId);//用户标识
        weiXinPrePay.setSpbillCreateIp(orderIp);//下单IP

        return weiXinPrePay;
    }
    
    /**
	 * 处理交易记录 如果交易记录是成功或者本地未支付,查询上游已支付,返回TRUE 如果上游支付结果为未支付,返回FALSE
	 *
	 * @param bankOrderNo
	 *            银行订单号
	 * @return
	 */
	@Override
	public boolean processingTradeRecord(String bankOrderNo) {

		RpTradePaymentRecord byBankOrderNo = rpTradePaymentRecordDao.getByBankOrderNo(bankOrderNo);
		if (byBankOrderNo == null) {
			LOG.info("不存在该银行订单号[{}]对应的交易记录", bankOrderNo);
			throw new TradeBizException(TradeBizException.TRADE_ORDER_ERROR, "非法订单号");
		}

		if (!TradeStatusEnum.WAITING_PAYMENT.name().equals(byBankOrderNo.getStatus())) {
			LOG.info("该银行订单号[{}]对应的交易记录状态为:{},不需要再处理", bankOrderNo, byBankOrderNo.getStatus());
			return true;
		} else {
			
			RpTradePaymentRecord rpTradePaymentRecord = rpTradePaymentRecordDao.getByBankOrderNo(bankOrderNo);
			if (rpTradePaymentRecord == null) {
				LOG.info("非法订单,订单不存在" + TradeBizException.TRADE_ORDER_ERROR);
				throw new TradeBizException(TradeBizException.TRADE_ORDER_ERROR, "非法订单,订单不存在");
			}
			
			String merchantNo = rpTradePaymentRecord.getMerchantNo();// 商户编号

			LOG.info("merchantNo=" + merchantNo);
			// 根据支付订单获取配置信息
			String fundIntoType = rpTradePaymentRecord.getFundIntoType();// 获取资金流入类型
			
			// 判断微信 支付宝 交易类型
			if (byBankOrderNo.getPayWayCode().equals(PayWayEnum.WEIXIN.name())) {
				String appid = null;
				String partnerKey = null;
				if (FundInfoTypeEnum.MERCHANT_RECEIVES.name().equals(fundIntoType)) {// 商户收款
					// 根据资金流向获取配置信息
					RpUserPayInfo rpUserPayInfo = rpUserPayInfoService.getByUserNo(merchantNo, PayWayEnum.WEIXIN.name());
					appid = rpUserPayInfo.getAppId();
					partnerKey = rpUserPayInfo.getPartnerKey();
				} else if (FundInfoTypeEnum.PLAT_RECEIVES.name().equals(fundIntoType)) {// 平台收款
					appid = PropertyConfigurer.getString("weixinpay.appId");
			        partnerKey = PropertyConfigurer.getString("weixinpay.partnerKey");
				} else {
					
				}
				Map<String, Object> resultMap = WeiXinPayUtils.orderQuery(appid, partnerKey, byBankOrderNo.getBankOrderNo());
				Object returnCode = resultMap.get("return_code");
				// 查询失败
				if (null == returnCode || "FAIL".equals(returnCode)) {
					return false;
				}
				// 当trade_state为SUCCESS时才返回result_code
				if ("SUCCESS".equals(resultMap.get("trade_state"))) {
					completeSuccessOrder(byBankOrderNo, byBankOrderNo.getBankTrxNo(), new Date(), "订单交易成功");
					return true;
				}
			} else if (byBankOrderNo.getPayWayCode().equals(PayWayEnum.ALIPAY.name())) {
				String partnerKey = null;
				if (FundInfoTypeEnum.MERCHANT_RECEIVES.name().equals(fundIntoType)) {// 商户收款
					// 根据资金流向获取配置信息
					RpUserPayInfo rpUserPayInfo = rpUserPayInfoService.getByUserNo(merchantNo, PayWayEnum.WEIXIN.name());
					partnerKey = rpUserPayInfo.getPartnerKey();
				} else if (FundInfoTypeEnum.PLAT_RECEIVES.name().equals(fundIntoType)) {// 平台收款
			        partnerKey = AlipayConfigUtil.partner;
				} else {
					
				}
				Map<String, String> resultMap = AliF2FPaySubmit.orderQuery(partnerKey, byBankOrderNo.getBankOrderNo());
				if (resultMap.isEmpty() || !"T".equals(resultMap.get("is_success"))) {
					return false;
				}
				// 当返回状态为“TRADE_FINISHED”交易成功结束和“TRADE_SUCCESS”支付成功时更新交易状态
				if ("TRADE_SUCCESS".equals(resultMap.get("trade_status")) || "TRADE_FINISHED".equals(resultMap.get("trade_status"))) {
					completeSuccessOrder(byBankOrderNo, byBankOrderNo.getBankTrxNo(), new Date(), "订单交易成功");
					return true;
				}
			}
			return false;
		}
	}
}