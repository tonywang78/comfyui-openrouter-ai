export namespace PasswordLoginApi {

    export interface Params {
        email: string
        password: string
    }
    
}

export namespace EmailLoginApi {

    
    export interface Params {
        email: string
        code: string
    }
    
}




export namespace GetVerificationCodeApi {

    
    export interface Params {
        email: string
        password: string
    }
    
}


export namespace RegisterApi {

    export interface Params {
        email: string
        code: string
        password: string
    }
}

export namespace ForgotPasswordApi {
    export interface Params {
        email: string
        code: string
        password: string
    }
}

export namespace PhoneVerificationCodeApi {
    export interface Params {
        phone: string
    }
}

export namespace PhoneLoginApi {
    export interface Params {
        phone: string
        code: string
    }
}

export namespace PhoneRegisterApi {
    export interface Params {
        phone: string
        code: string
        password?: string
        nickname?: string
    }
}

export namespace WechatBindPhoneApi {
    export interface Params {
        bindTicket: string
        phone: string
        code: string
    }
}

export namespace WechatLoginStateApi {
    export interface Response {
        state: string
        appId: string
        redirectUri: string
    }
}

export namespace WechatPollApi {
    export interface Response {
        status: 'pending' | 'success' | 'need_bind'
        token?: string
        bindTicket?: string
    }
}

