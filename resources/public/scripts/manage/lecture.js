const get_grouplist_url = "/manage/api/get-grouplist";
const get_courselist_url = "/manage/api/get-raw-courselist";
const get_priv_info_url = "/manage/api/get-privilege-info?gid=";
const set_privilege_url = "/manage/api/set-privilege";

var update_info = [];

window.addEventListener('DOMContentLoaded', function() {
    fetch(get_grouplist_url)
        .then((res) => {return res.json();})
        .then((result) => {
            let select = document.getElementById("group_selection");
            let options = result.map((grp) => {
                let option = document.createElement("option");
                option.text = grp.groupname;
                option.value = grp.gid;
                return option;
            });
            options.forEach((opt) => {select.appendChild(opt);});
        })
        .catch((e) => {
            alert("Failed to fetch information");
            console.log(e);
        });
});

function build_table_info(lecinfo, privinfo) {
    priv_data = lecinfo.map((course) => {
        let dbinfo = privinfo.find((data) => data["course_id"] === course["id"]);
        if(typeof dbinfo === "undefined") {
            return {"title": course["title"],
                    "cid": course["id"],
                    "show": false,
                    "attend": false,
                    "execute": false};
        }else{
            return {"title": course["title"],
                    "cid": course["id"],
                    "show": 'show_p' in dbinfo ? dbinfo["show_p"] : false,
                    "attend": 'attend_p' in dbinfo ? dbinfo["attend_p"] : false,
                    "execute": 'execute_p' in dbinfo ? dbinfo["execute_p"] : false};
        }
    });
    return priv_data;
}

function clear_tbody(target_tbody) {
    while (target_tbody.firstChild) {
        target_tbody.removeChild(target_tbody.firstChild);
    }
}

function apply_table(gid, table_info) {
    let priv_table = document.getElementById("privilege_table");
    let priv_thead = document.getElementById("privilege_table_head");
    let priv_tbody = document.getElementById("privilege_table_body");
    
    clear_tbody(priv_tbody);

    table_info.forEach((col) => {
        let tr = priv_tbody.insertRow();
        let tr_title = document.createElement("th");
        
        tr_title.innerHTML = col["title"];
        tr.appendChild(tr_title);
        
        let id_cell = tr.insertCell();
        id_cell.appendChild(document.createTextNode(col["cid"]));
        
        let show_cell = tr.insertCell();
        let input_show = document.createElement("input");
        input_show.type = "checkbox";
        input_show.checked = col['show'];
        input_show.addEventListener('change', (event) => {
            if (event.currentTarget.checked) {
                update_info.push({"gid": gid, "cid": col["cid"], "show": true});
            } else {
                update_info.push({"gid": gid, "cid": col["cid"], "show": false});
            }
        });
        show_cell.appendChild(input_show);
        
        let attend_cell = tr.insertCell();
        let input_attend = document.createElement("input");
        input_attend.type = "checkbox";
        input_attend.checked = col['attend'];
        input_attend.addEventListener('change', (event) => {
            if (event.currentTarget.checked) {
                update_info.push({"gid": gid, "cid": col["cid"], "attend": true});
            } else {
                update_info.push({"gid": gid, "cid": col["cid"], "attend": false});
            }
        });
        attend_cell.appendChild(input_attend);
        
        let execute_cell = tr.insertCell();
        let input_execute = document.createElement("input");
        input_execute.type = "checkbox";
        input_execute.checked = col['execute'];
        input_execute.addEventListener('change', (event) => {
            if (event.currentTarget.checked) {
                update_info.push({"gid": gid, "cid": col["cid"], "execute": true});
            } else {
                update_info.push({"gid": gid, "cid": col["cid"], "execute": false});
            }
        });
        execute_cell.appendChild(input_execute);
    });
}

function render_privilege_table(gid) {
    Promise.all([fetch(get_courselist_url), fetch(get_priv_info_url + gid)])
        .then((res) => {
            return Promise.all(res.map(function (response) {
                return response.json();
            }));
        })
        .then((result) => {
            let table_info = build_table_info(result[0], result[1]);
            apply_table(gid, table_info);
        })
        .catch((e) => {
            alert("Failed to fetch information");
            console.log(e);
        });
}

function select_group() {
    let selected_gid = document.getElementById("group_selection").value;
    let gid = parseInt(selected_gid, 10);
    if(!isNaN(gid)){
        console.log(`selected = ${gid}`);
        update_info = [];
        if(gid > 0) {
            document.getElementById("privi_viewer_area").hidden = false;
            render_privilege_table(gid);
        }else {
            document.getElementById("privi_viewer_area").hidden = true;
        }
    }else{
        alert("Something seems to be wrong with the selected item");
    }
}

function update() {
    // send update information
	fetch(set_privilege_url, {
        method: "post",
        headers: {'Content-Type': "application/json"},
        body: JSON.stringify(update_info)})
        .then(res => res.text())
        .then(result => {
            console.log(result);
            (async () => {
                await new Promise(resolve => {
                    setTimeout(resolve, 1000);
                    select_group();
                });
            })();
        });
}
