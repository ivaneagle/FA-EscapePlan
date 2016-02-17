/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package faescapeplan;

import java.util.Map;

/**
 *
 * @author Owner
 */
public class UserData {
    private String name;
    private Map<String, String> cookies;
    private boolean loggedIn;
    
    public UserData() {
        //insert constructor here
        this.setLoginState(false);
    }
    
    public String getName() {
        String userName = this.name;
        return userName;
    }
    
    public void setName(String userName) {
        this.name = userName;
    }
    
    public Map<String, String> getCookies() {
        Map<String, String> userCookies = this.cookies;
        return userCookies;
    }
    
    public void setCookies(Map<String, String> userCookies) {
        this.cookies = userCookies;
    }
    
    public boolean getLoginState() {
        boolean login = this.loggedIn;
        return login;
    }
    
    public void setLoginState(boolean login) {
        this.loggedIn = login;
    }
}
