import {createReducer} from 'common/utils/reducerUtils';
import { GUILDS_FOUND } from './guildsListConstants';


const initialState = [];


export function loadResults(state = {}, {guilds}) { 
   return guilds
}

export default createReducer(initialState, {
  [GUILDS_FOUND]: loadResults
})
