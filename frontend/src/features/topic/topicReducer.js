import {createReducer} from 'common/utils/reducerUtils';
import { FIND_TOPIC, TOPIC_FOUND } from './topicConstants';


const initialState = { };

export function updateTopic(state = {}, payload= {}) {
    return {...payload}
}

export function loadTopic(state = {}, payload) { 
  return {...payload}
}

export default createReducer(initialState, {
  [FIND_TOPIC]: updateTopic,
  [TOPIC_FOUND]: loadTopic
})
