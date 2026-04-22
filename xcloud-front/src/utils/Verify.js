const regs = {
    email: /^[1-9][0-9]{6,}@qq\.com$|^[A-Za-z0-9._%+-]+@gmail\.com$|^[A-Za-z0-9._%+-]+@163\.com$/,
    number:/^(0|[1-9][0-9]*)$/,
    password: /^(?=.*[A-Za-z])(?=.*\d).{8,}$/,
    shareCode:/^[A-Za-z0-9]+$/,
    nickName:/^[\u4e00-\u9fa5a-zA-Z0-9_]{1,14}$/,
}

const verify = (rule,value,reg,callback)=>{
    if (value){
        if (reg.test(value)){
            callback()
        }else {
            callback(new Error(rule.message))
        }
    }else {
        callback()
    }
}

export default {
    email: (rule,value,callback)=>{
        return verify(rule,value,regs.email,callback)
    },
    number: (rule,value,callback)=>{
        return verify(rule,value,regs.number,callback)
    },
    password: (rule,value,callback)=>{
        return verify(rule,value,regs.password,callback)
    },
    shareCode: (rule,value,callback)=>{
        return verify(rule,value,regs.shareCode,callback)
    },
    nickName: (rule,value,callback)=>{
        return verify(rule,value,regs.nickName,callback)
    },
}

