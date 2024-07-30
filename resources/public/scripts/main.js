// for /

function remove_div_elements(divelem) {
    while (divelem.firstChild) {
        divelem.removeChild(divelem.firstChild);
    }
}

function set_page_for(user) {
    console.log("user = " + user);
    let session_btns = document.getElementById("session_btns");
    remove_div_elements(session_btns);

    let mypage_btn = document.createElement("a");
    mypage_btn.setAttribute("class", "button is-info");
    mypage_btn.setAttribute("href", "/my");
    mypage_btn.innerHTML = "My page";
    session_btns.appendChild(mypage_btn);
    
    let logout_btn = document.createElement("a");
    logout_btn.setAttribute("class", "button is-light");
    logout_btn.setAttribute("href", "/logout");
    logout_btn.innerHTML = "Logout";
    session_btns.appendChild(logout_btn);

    let start_btn = document.createElement("a");
    start_btn.setAttribute("class", "button is-light is-primary is-large");
    start_btn.setAttribute("href", "/lecture");
    start_btn.innerHTML = "Enter";
    document.getElementById("main-contents").appendChild(start_btn);
    
}

function set_page_for_everybody(){
    let session_btns = document.getElementById("session_btns");
    remove_div_elements(session_btns);
    
    let regi_btn = document.createElement("a");
    regi_btn.setAttribute("class", "button is-primary1");
    regi_btn.setAttribute("href", "/registration");
    regi_btn.innerHTML = "Registration";
    session_btns.appendChild(regi_btn);
    
    let login_btn = document.createElement("a");
    login_btn.setAttribute("class", "button is-light");
    login_btn.setAttribute("href", "/login");
    login_btn.innerHTML = "Login";
    session_btns.appendChild(login_btn);
}

function append_manage_link(){
    let session_btns = document.getElementById("session_btns");
    let manage_btn = document.createElement("a");
    manage_btn.setAttribute("class", "button is-warning");
    manage_btn.setAttribute("href", "/manage");
    manage_btn.innerHTML = "Management page";
    session_btns.appendChild(manage_btn);
}

window.addEventListener('DOMContentLoaded', function() {
    fetch('/whoami')
        .then((response) => {
            return response.text();
        })
        .then((result) => {
            let user = result;
            if(user !== "") {
                set_page_for(user);
            }else{
                set_page_for_everybody();
            }
        })
        .catch((error) => {
            alert("Something wrong...");
            console.log(error);
        });

    fetch('/iadmin')
        .then((response) => {
            return response.json();
        })
        .then((result) => {
            if(result["answer"]){
                append_manage_link();
            }
        })
        .catch((error) => {
            alert("Something wrong...");
            console.log(error);
        });
});
