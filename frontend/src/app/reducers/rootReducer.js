import {combineReducers} from 'redux'

import {reduceReducers} from 'common/utils/reducerUtils'
import guildsListReducer from 'features/guildsList/guildsListReducer'
import topicReducer from 'features/topic/topicReducer' 
import authenticationReducer from 'features/authentication/authenticationReducer'

const combinedReducer = combineReducers({
  guilds: guildsListReducer,
  topic: topicReducer,
  auth: authenticationReducer 
})

const rootReducer = reduceReducers(
  combinedReducer
)

export default rootReducer
