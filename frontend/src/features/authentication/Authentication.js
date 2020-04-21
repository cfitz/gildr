import React, { PureComponent } from 'react';
import { withStyles } from '@material-ui/core/styles';                                                                                          
import { connect } from 'react-redux';
import { Redirect } from 'react-router-dom';
import { authorize } from './authenticationActions';
import { Paper, Grid, TextField, Button  } from '@material-ui/core';
import { Face, Fingerprint } from '@material-ui/icons'

const styles = theme => ({
  margin: {
      margin: theme.spacing.unit * 2,
  },
  padding: {
      padding: theme.spacing.unit
  },
  root: {
    padding: theme.spacing.unit,
    paddingTop: theme.spacing.unit * 4
  }
});


class Login extends PureComponent {

  constructor(props) {
    super(props);
    this.state = { name: '', password: '' };
    this.onChange = this.onChange.bind(this);
    this.onSubmit = this.onSubmit.bind(this);
  }

  onChange(input, {target}) {
    const {value} = target;
    this.setState({ [input]: value });
  }

  onSubmit() {
    const { name, password } = this.state;
    this.props.dispatch(authorize(name, password));
  }

  render() {
    const { classes, error, token } = this.props;

    if (token) {
      return <Redirect to="/" />;
    }

    return (
     <Grid container justify="center" className={classes.root}>
      <Paper className={classes.padding}>
        <div className={classes.margin}>
          <Grid container spacing={8} alignItems="center">
              <Grid item>
                  <Face />
              </Grid>
              <Grid item md={true} sm={true} xs={true}>
                <TextField id="name" label="Username"  fullWidth autoFocus required  value={this.state.name}
                          onChange={this.onChange.bind(this, 'name')}
                />
              </Grid>
          </Grid>
          <Grid container spacing={8} alignItems="flex-end">
              <Grid item>
                  <Fingerprint />
              </Grid>
              <Grid item md={true} sm={true} xs={true}>
              <TextField
          label="Password"
          type="password"
          autoComplete="current-password"
          onChange={this.onChange.bind(this, 'password')}
        />
              </Grid>
          </Grid>
          <Grid container justify="center" style={{ marginTop: '10px' }}>
              <Button variant="outlined" color="primary" style={{ textTransform: "none" }} onClick={this.onSubmit}>Login</Button>
          </Grid>
      </div>
  </Paper>
  </Grid>
    );
  }
}

const mapStateToProps = ({auth}, ownProps) => {
  return {...ownProps, auth };
}

export default withStyles(styles)(connect(mapStateToProps)(Login));
