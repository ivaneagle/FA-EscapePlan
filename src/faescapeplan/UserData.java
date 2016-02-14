/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package faescapeplan;

/**
 *
 * @author Owner
 */
public class UserData {
    private String name;
    private String token;
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
    
    public String getToken() {
        String userToken = this.token;
        return userToken;
    }
    
    public void setToken(String userToken) {
        this.token = userToken;
    }
    
    public boolean getLoginState() {
        boolean login = this.loggedIn;
        return login;
    }
    
    public void setLoginState(boolean login) {
        this.loggedIn = login;
    }
}
