import axios from 'axios';

const BACKEND_URL = process.env.REACT_APP_BACKEND_URL;

export const login = ({name, password}) => {
  const body = {name, password}
  return axios.post(`${BACKEND_URL}/login`, body).then(response => response.data )
}

export const listGuilds = () => {
   const token = localStorage.getItem('token');
   if (token) {
     const config = {
        headers: { Authorization: `Bearer ${token}` }
      };
      return axios.get(`${BACKEND_URL}/v1/guilds`, config).then(response => response.data )
    }
}

// export const getTopic = (id) => { 
//   console.log(id)
//   return axios.get(`${BACKEND_URL}/api/v1/topics/${id}`).then(response => response.data )
// }
