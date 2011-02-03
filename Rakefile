require 'rake/testtask'
require 'json' # Please use SYCK
require 'yaml'

namespace :build do
  
  desc "Build JSON from YAML"
  task :json do
    Dir.glob("specs/*").collect{|f| ff=f.gsub("yml","json"); File.open(ff,'w'){|x| x << YAML.parse(File.open(f)).transform.to_json}}
  end
  
end