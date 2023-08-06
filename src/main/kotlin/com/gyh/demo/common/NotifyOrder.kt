package com.gyh.demo.common

/**
 * Created by gyh on 2020/5/15.
 */
object NotifyOrder {
    const val connectSucceed = -99      // 连接成功通知
    const val differentPlaceLogin = -10 // 用户账号在其他地方登录
    const val pushBuyOrder = -8         // 推送买/卖单更新
    const val pushTradeInfo = -7        // 推送交易信息更新
    const val notifyRoomClose = -6      // 房间关闭通知
    const val pushVB = -5               // 买一卖一推送
    const val userMsg = -3              // 私聊消息通知
    const val groupMag = -2             // 群消息通知
    const val errorNotify = -1          // 错误通知
    const val requestReq = 0            // 正常响应
}