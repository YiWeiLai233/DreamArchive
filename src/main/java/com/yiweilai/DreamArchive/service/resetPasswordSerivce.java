package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.mapper.resetPasswordMapper;
import com.yiweilai.DreamArchive.util.passwordEncrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class resetPasswordSerivce {
    @Autowired
    passwordEncrypt pE;
    @Autowired
    resetPasswordMapper resetpasswordmapper;
    //可以通过正则表达来判断是账号还是email 通过判断是否有@这个字符
    public String resetPassword(String identifier,String newPassword) throws Exception {
        //如果输入的字符串带@就判断是Email
        if (identifier.matches("[*@.*]")){
            resetpasswordmapper.resetByEmail(pE.encrypt(newPassword));
            return "200";
            //如果是纯数字就是账号
        }else if(identifier.matches("[\\d+]")){
            resetpasswordmapper.resetByUsername(pE.encrypt(newPassword));
            return "200";
        }
        return "Error";
    }
}
