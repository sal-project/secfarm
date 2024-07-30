const userlist_url = "/manage/api/get-userlist";
const modify_url = "/manage/api/modify-userinfo";
var userinfo = [];

function clear_tbody(target_tbody) {
    while (target_tbody.firstChild) {
        target_tbody.removeChild(target_tbody.firstChild);
    }
}

function make_grouplist_text(groupinfo) {
    let result = "";
    if(groupinfo !== null) {
        groupinfo.forEach((group) => {
            if(result !== ""){
                result += "," + group.groupname;
            }else{
                result = group.groupname;
            }
        });
    }
    return result;
}

function make_grouplist(grouptext) {
    const groups = grouptext.split(",");
    return groups
        .map((group) => {return group.trim();})
        .filter((group) => {return group.length !== 0;});
}

function build_table(data){
    let user_table = document.getElementById("user_table");
    let user_thead = document.getElementById("user_table_head");
    let user_tbody = document.getElementById("user_table_body");

    clear_tbody(user_tbody);

    data.forEach((col) => {
        let tr = user_tbody.insertRow();
        let tr_uid = document.createElement("th");
        
        tr_uid.innerHTML = col["uid"];
        tr.appendChild(tr_uid);

        let username_cell = tr.insertCell();
        username_cell.innerHTML = col["username"];
        
        let contact_cell = tr.insertCell();
        contact_cell.innerHTML = col["contact"];

        let group_cell = tr.insertCell();
        let group_text = document.createElement("input");
        group_text.type = "text";
        group_text.value = make_grouplist_text(col["group"]);
        group_text.addEventListener("change", (event) => {
            document.getElementById("apply_btn").removeAttribute("disabled");
        });
        group_cell.appendChild(group_text);

        let locked_cell = tr.insertCell();
        let locked = document.createElement("input");
        locked.type = "checkbox";
        if(col["locked"]) {
            locked.checked = true;
        }
        locked.addEventListener("change", (event) => {
            document.getElementById("apply_btn").removeAttribute("disabled");
        });
        locked_cell.appendChild(locked);
    });
}

function create_userlist(){
    fetch(userlist_url)
        .then((res) => {return res.json();})
        .then((result) => {
            userinfo = result;
            build_table(result);
        })
        .catch((e) => {
            alert("Failed to fetch user information");
            console.log(e);
        });
}

function apply(){
    let user_tbody = document.getElementById("user_table_body");
    const tbody_nodes = user_tbody.childNodes;
    const updates = Array.prototype.map.call(tbody_nodes, function(cols) {
        const uid = Number(cols.childNodes[0].textContent);
        const groups = make_grouplist(cols.childNodes[3].childNodes[0].value);
        const locked = cols.childNodes[4].childNodes[0].checked;
        return {"uid": uid,
                "groups": groups,
                "locked": locked};
    });

    console.log(updates);
    fetch(modify_url,
          {method: "POST",
           headers: {"Content-Type": "application/json"},
           body: JSON.stringify(updates)})
        .then((res) => {return res.json();})
        .then((result) => {
            console.log(result);
            document.getElementById("apply_btn").setAttribute("disabled", true);
            location.reload();
        })
        .catch((e) => {
            alert("Failed to upload changes");
            console.log(e);
        });
    
}

window.addEventListener('DOMContentLoaded', function() {
    create_userlist();
});
