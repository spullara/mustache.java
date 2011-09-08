var sys = require('sys');
var m = require("mustache");
var fs = require("fs");

var items = [{name:"red", current:true, url:"#Red", link:false},
  {name:"green", current:false, url:"#Green", link:true},
  {name:"blue", current:false, url:"#Blue", link:true}];

var view = {
  header : "Colors",
  item : items,
  list:function() {
    return items.length != 0;
  },
  empty:function() {
    return items.length == 0;
  }
};

fs.readFile("src/test/resources/complex.html", function(err, data) {
  if (err) throw err;
  var template = data.toString();
  var html = m.to_html(template, view);
  var start = new Date().getTime();
  var total = 0;
  sys.puts(m.to_html(template, view));
  while(true) {
    m.to_html(template, view);
    total++;
    if (new Date().getTime() - start > 5000) {
      break;
    }
  }
  sys.puts(total / 5);
});

