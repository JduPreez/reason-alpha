- Fix unit tests `reason-alpha.data-structures-test`
- There's a problem with the trade pattern graph when a parent's parent is assigned as one of the children of the parent
|> DONE: the simplest solution is to allow the graph to be only 2 levels deep & distinguish between the main pattern and the
   sub-pattern
|> DONE: So for all parent patterns disable editing of the Parent column: 
https://stackoverflow.com/questions/51652703/editable-and-non-editable-some-ag-grid-cells-dynamically
|> TODO: For the select values, only list main parent patterns. I.e. where the ancestor path is only 1 level deep  |
|> TODO: Display parent name in drop down list                                                                     |
