class Complex
  def header
    "Colors"
  end

  def item
    [{:name=>"red",:current=>true,:url=>"#Red",:link=>false},
     {:name=>"green",:current=>false,:url=>"#Green",:link=>true},
     {:name=>"blue",:current=>false,:url=>"#Blue",:link=>true}]
  end

  def list
    item.size != 0
  end

  def empty
    item.size == 0
  end
end
Complex.new