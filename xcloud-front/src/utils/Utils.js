export default {
    sizeToStr:(limit) =>{
        var size = "";
        if(limit<0.1*1024){
            size = limit.toFixed(2) + "B"                       //小于0.1KB,转化成B
        }else if (limit<0.1*1024*1024){
            size = (limit/1024).toFixed(2) + "KB"              //小于0.1MB,转化成KB
        }else if(limit<0.1*1024*1024*1024){
            size = (limit/(1024*1024)).toFixed(2) + "MB"              //小于0.1G,转化成MB
        }else {
            size = (limit/(1024*1024*1024)).toFixed(2) + "GB"              //其余转化成GB
        }
        var sizeStr = size + "";                                        //转成字符串
        var index = sizeStr.indexOf(".")                                //获取小数点处的索引
        var dou = sizeStr.substr(index+1,2)                       //获取小数点后两位的值
        if (dou == "00"){                                                            //判断小数点后两位是否为00，如果是删除
            return sizeStr.substring(0,index) + sizeStr.substr(index + 3,2)
        }
        return size;
    }
}