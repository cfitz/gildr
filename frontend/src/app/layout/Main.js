import React from 'react'
import {Switch, Route} from 'react-router-dom'
import {withStyles} from '@material-ui/core/styles'

import IndexPage from 'pages/IndexPage'
import Login from 'pages/LoginPage'

import {createBrowserHistory} from 'history'

export const history = createBrowserHistory({
      basename: process.env.PUBLIC_URL
})

const styles = theme => ({
})

const Main = props => {
  const {classes} = props
  return (
    <main className={classes.root}>
      <Switch>
        <Route exact path="/" component={IndexPage}/>
        <Route path="/login" component={Login} />
      </Switch>
    </main>
  )
}
export default withStyles(styles)(Main)
