import type {
    PasswordLoginApi,
    EmailLoginApi,
    RegisterApi,
    ForgotPasswordApi,
    GetVerificationCodeApi,
    PhoneVerificationCodeApi,
    PhoneLoginApi,
    PhoneRegisterApi,
    WechatBindPhoneApi,
    WechatLoginStateApi,
    WechatPollApi,
    CaptchaApi
} from './types'
import { post, get } from '@/utils/requestUtil'

export const authApi = {
    reqCaptcha: () => {
        return get<CaptchaApi.Response>('/auth/captcha')
    },
    reqPasswordLogin: (params: PasswordLoginApi.Params) => {
        return post<string>('/auth/password-login', params)
    },
    reqEmailLogin: (params: EmailLoginApi.Params) => {
        return post<string>('/auth/email-login', params)
    },
    reqRegister: (params: RegisterApi.Params) => {
        return post<void>('/auth/register', params)
    },
    reqForgotPassword: (params: ForgotPasswordApi.Params) => {
        return post<void>('/auth/forgot-password', params)
    },
    reqGetVerificationCode: (params: GetVerificationCodeApi.Params) => {
        return post<void>('/auth/verification-code', { email: params.email })
    },
    reqPhoneVerificationCode: (params: PhoneVerificationCodeApi.Params) => {
        return post<void>('/auth/phone-verification-code', params)
    },
    reqPhoneLogin: (params: PhoneLoginApi.Params) => {
        return post<string>('/auth/phone-login', params)
    },
    reqPhoneRegister: (params: PhoneRegisterApi.Params) => {
        return post<void>('/auth/phone-register', params)
    },
    reqWechatLoginState: () => {
        return post<WechatLoginStateApi.Response>('/auth/wechat/login-state')
    },
    reqWechatPoll: (state: string) => {
        return get<WechatPollApi.Response>('/auth/wechat/poll', { state })
    },
    reqWechatBindPhone: (params: WechatBindPhoneApi.Params) => {
        return post<string>('/auth/wechat/bind-phone', params)
    },
    reqLogout: () => {
        return post<void>('/auth/logout')
    }
}
