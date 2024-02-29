(ns circleci-test.config)

{:selectors {:all         (constantly true)
             :integration :integration
             :default     (complement :integration) }}
