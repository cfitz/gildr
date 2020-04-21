import React, { PureComponent } from 'react';
import { connect } from 'react-redux';
import { NavLink } from 'react-router-dom';
import { withStyles } from '@material-ui/core/styles';
import Typography from '@material-ui/core/Typography';

const styles = theme => ({
    link:{
        color: 'inherit'                                                                                                                            
    }
})

export const LoginSection  = ({token, classes}) => {
  const section = token ? <div> You are logged in.</div> : <NavLink to="/login" className={classes.link}>Login</NavLink> 

  return(
    <Typography className={classes.title} variant="h6" color="inherit" noWrap>
        {section}
    </Typography>)
}


const mapStateToProps = (state) => ({
    token: state.auth.token
  });
  
export default withStyles(styles)(connect(mapStateToProps)(LoginSection));
